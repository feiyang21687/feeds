package com.feiyang.feeds.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.feiyang.feeds.Fixture;
import com.feiyang.feeds.model.FeedContent;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

public class GAEFeedContentServiceImplTest {
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalTaskQueueTestConfig(),
			new LocalDatastoreServiceTestConfig());

	private Fixture fixture = new Fixture();

	@Before
	public void setUp() throws Exception {
		helper.setUp();
		fixture.setUp(DatastoreServiceFactory.getDatastoreService());
	}

	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	@Test
	public void testLatestContent() {
		List<FeedContent> contents = fixture.feedContentServiceFixture.latestContent(fixture.feedFixture.iterator()
				.next().getSite(), 15);
		assertEquals(2, contents.size());
	}

}
