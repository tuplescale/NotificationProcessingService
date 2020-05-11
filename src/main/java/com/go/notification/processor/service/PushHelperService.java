package com.go.notification.processor.service;

import java.util.List;

import com.ne.commons.notification.vo.User;

public interface PushHelperService {
	List<User> getOnlineUsers(List<User> oldUserList);
}
