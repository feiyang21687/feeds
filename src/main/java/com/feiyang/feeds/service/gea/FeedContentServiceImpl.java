package com.feiyang.feeds.service.gea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.feiyang.feeds.model.FeedContent;
import com.feiyang.feeds.model.FeedContentEntityHelper;
import com.feiyang.feeds.service.FeedContentService;
import com.feiyang.feeds.util.FeedUuidService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@Component
public class FeedContentServiceImpl implements FeedContentService {
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	@Override
	public List<FeedContent> latestContent(String site, int limit) {
		if (!StringUtils.hasText(site) || limit <= 0) {
			return Collections.emptyList();
		}

		Filter siteFilter = new FilterPredicate("site", FilterOperator.EQUAL, site);
		PreparedQuery pq = datastore.prepare(new Query(FeedContentEntityHelper.kind()).setFilter(siteFilter));

		List<Entity> entities = pq.asList(FetchOptions.Builder.withLimit(limit));
		List<FeedContent> rs = new ArrayList<>(entities.size());
		for (Entity entity : entities) {
			rs.add(FeedContentEntityHelper.toFeedContent(entity));
		}
		return rs;
	}

	@Override
	public List<FeedContent> queryContent(Collection<Long> feedIds) {
		if (CollectionUtils.isEmpty(feedIds)) {
			return Collections.emptyList();
		}

		List<Key> keys = FeedContentEntityHelper.keys(feedIds);
		Filter filter = new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, keys);
		PreparedQuery pq = datastore.prepare(new Query(FeedContentEntityHelper.kind()).setFilter(filter));
		List<Entity> entities = pq.asList(FetchOptions.Builder.withLimit(keys.size()));
		return FeedContentEntityHelper.toFeedContent(entities);
	}

	@Override
	public List<FeedContent> saveConent(List<FeedContent> contents) {
		if (CollectionUtils.isEmpty(contents)) {
			return contents;
		}

		List<FeedContent> feedContents = new ArrayList<>(contents);

		// reset the feed id.
		List<Key> feedIds = new ArrayList<>(feedContents.size());
		for (FeedContent feedContent : feedContents) {
			feedContent.setId(FeedUuidService.id(feedContent));
			feedIds.add(FeedContentEntityHelper.key(feedContent.getId()));
		}

		// check duplicate and ignore already stored content.
		PreparedQuery pq = datastore.prepare(new Query(FeedContentEntityHelper.kind()).setFilter(
		        new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.IN, feedIds)).setKeysOnly());
		List<Entity> alreadyStoredEntities = pq.asList(FetchOptions.Builder.withLimit(feedContents.size()));
		FeedContent removed = new FeedContent();
		for (Entity entity : alreadyStoredEntities) {
			removed.setId(entity.getKey().getId());
			feedContents.remove(removed);
		}

		// save the fresh feed conent.
		List<Entity> toBeStoredEntities = new ArrayList<>(feedContents.size());
		for (FeedContent feedContent : feedContents) {
			toBeStoredEntities.add(FeedContentEntityHelper.toEntity(feedContent));
		}
		datastore.put(toBeStoredEntities);
		return feedContents;
	}
}
