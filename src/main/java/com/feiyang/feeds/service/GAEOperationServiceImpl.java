package com.feiyang.feeds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.feiyang.feeds.model.Category;
import com.feiyang.feeds.model.CategoryEntityHelper;
import com.feiyang.feeds.model.FeedContent;
import com.feiyang.feeds.model.Subscribe;
import com.feiyang.feeds.model.SubscribeEntityHelper;
import com.feiyang.feeds.model.User;
import com.feiyang.feeds.util.UuidGenerator;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@Component
public class GAEOperationServiceImpl implements OperationService {
	private static final int NEW_SUBSCRIBE_MAX_CONTENT = 15;

	private static final Logger LOG = Logger.getLogger(GAEOperationServiceImpl.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	@Autowired(required = true)
	private FeedContentService feedContentService;

	@Override
	public boolean createCategory(User user, String name) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException(String.format("category name=%s", name));
		}

		long categoryId = UuidGenerator.INSTANCE.next();
		Category category = new Category(user, categoryId, name, null);
		Entity entity = category.toEntity();
		Key key = datastore.put(entity);
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("user create category:%s", key));
		}
		return true;
	}

	@Override
	public Map<Subscribe, List<FeedContent>> addSubscribe(User user, long categoryId, String site) {
		if (categoryId <= 0) {
			throw new IllegalArgumentException(String.format("categoryId=%d, site=%s", categoryId, site));
		}

		// query category.
		Key categoryFilterKey = CategoryEntityHelper.key(user.getUid(), categoryId);
		PreparedQuery pq = datastore.prepare(new Query(categoryFilterKey));
		Entity entity = pq.asSingleEntity();
		if (entity == null) {
			// TODO should define own exception.
			return null;
		}

		// check this site already exists in category. if so, just return true;
		Category category = CategoryEntityHelper.toCategory(entity);
		List<Long> subscribes = category.getSubscribes();
		if (subscribes == null) {
			subscribes = new ArrayList<>();
			category.setSubscribes(subscribes);
		} else if (checkAlreadySubscribed(category, site)) {
			LOG.info(String.format("user(%d) category(%s) has already subscribe site(%s)", user.getUid(),
					category.getName(), site));
			return null;
		}

		// fetch latest feed content.
		List<FeedContent> contents = feedContentService.latestContent(site, NEW_SUBSCRIBE_MAX_CONTENT);
		List<Long> feedIds = new ArrayList<>(contents.size());
		for (FeedContent feedContent : contents) {
			feedIds.add(feedContent.getId());
		}

		// save the new subscribe to storage.
		Subscribe subscribe = new Subscribe(UuidGenerator.INSTANCE.next(), site, user.getUid(), feedIds);
		datastore.put(SubscribeEntityHelper.toEntity(subscribe));

		Map<Subscribe, List<FeedContent>> rs = new TreeMap<>();
		rs.put(subscribe, contents);
		return rs;
	}

	private boolean checkAlreadySubscribed(Category category, String site) {
		List<Key> keys = SubscribeEntityHelper.keys(category.getSubscribes());
		Filter keyFilter = new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, keys);
		Filter filter = CompositeFilterOperator.and(keyFilter, new FilterPredicate("site", FilterOperator.EQUAL, site));
		PreparedQuery pq = datastore.prepare(new Query(SubscribeEntityHelper.kind()).setFilter(filter).setKeysOnly());
		int count = pq.countEntities(FetchOptions.Builder.withLimit(1));
		return count > 0;
	}
}
