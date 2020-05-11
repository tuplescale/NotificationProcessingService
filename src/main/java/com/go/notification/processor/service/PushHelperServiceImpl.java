package com.go.notification.processor.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.go.notification.processor.dao.ProcessingDAO;
import com.ne.commons.notification.vo.User;

/**
 * Service to handle push notification related tasks
 * @author santhosh.gudla
 *
 */
@Service
public class PushHelperServiceImpl implements PushHelperService {
	
	private static final Logger log = LoggerFactory.getLogger(PushHelperServiceImpl.class);
	
	@Autowired
	JedisService jedisService;
	
	@Autowired
	ProcessingDAO processingDAO;

	/**
	 * From the give users list get only online users
	 * and for offline users update push subscription status to 0
	 */
	@Override
	public List<User> getOnlineUsers(List<User> oldUserList) {
		List<User> newUserList = new ArrayList<>();
		for (User user : oldUserList) {
			try {
				String status = jedisService.getUserStatusFromRedis(user.getUserId());
				log.info("User Status received from redis : "+status+"For user Id :"+user.getUserId());
				if(status.equalsIgnoreCase("offline")) {
					processingDAO.unSubscribePushNotification(user.getUserId());
				} else if(status.equalsIgnoreCase("online") || status.equalsIgnoreCase("away")) {
					newUserList.add(user);
				}
			} catch (SQLException e) {
				log.error("Error while getting user status from redis:"+user.getUserId(), e);
			}
		}
		return newUserList;
	}

}
