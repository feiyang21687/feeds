package com.feiyang.feeds.service;

import java.util.List;

import com.feiyang.feeds.model.FeedContent;

public interface CrawlerService {
	List<FeedContent> crawl(String site);
}
