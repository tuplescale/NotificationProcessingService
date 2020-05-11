package com.go.notification.processor.service;

import java.sql.SQLException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.go.notification.processor.constants.JedisEnum;
import com.ne.commons.redis.config.RedisConfig;

import redis.clients.jedis.Jedis;

@Service
public class JedisService {

	@Autowired
	RedisConfig redisConfig;
	
	@Value("${notification.time}")
	int notificationTime;

	private static final Logger log = LoggerFactory.getLogger(JedisService.class);

	public String getUserStatusFromRedis(long userId) throws SQLException {
		String userStatus = "offline";
		Jedis jedis = redisConfig.getConnection();

		long userTimeStamp = 0L;
		try {
			String cacheTimeStamp = jedis.get(JedisEnum.USERPRESENCE + "_" + String.valueOf(userId));
			if (cacheTimeStamp != null) {
				userTimeStamp = Long.parseLong(cacheTimeStamp);
				DateTime dt = new DateTime(DateTimeZone.UTC);
				long currentTimeStamp = dt.getMillis() / 1000;
				if (userTimeStamp == 0) {
					userStatus = "offline";
				} else if (currentTimeStamp - userTimeStamp < 300) {
					userStatus = "online";
				} else if ((currentTimeStamp - userTimeStamp) < 28800) {
					userStatus = "away";
				} else if ((currentTimeStamp - userTimeStamp) >= 28800) {
					userStatus = "offline";
				}
			}
		} catch (Exception e) {
			log.error("Exception in getting user presence from redis:", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		return userStatus;
	}

	public String getNotificationUserConv(String conversationId, long userId) {
		String value = null;
		Jedis jedis = redisConfig.getConnection();
		try {
			value = jedis
					.get(JedisEnum.NOTIFICATION + "_" + String.valueOf(conversationId) + "_" + String.valueOf(userId));

		} catch (Exception e) {
			log.error("Exception in getting notification conversation information:", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return value;
	}

	public int getUserConvTTL(String conversationId, long userId) {
		int value = 0;
		Jedis jedis = redisConfig.getConnection();
		try {
			String data = jedis
					.get(JedisEnum.TTL + "_" + String.valueOf(conversationId) + "_" + String.valueOf(userId));
			if (data != null) {
				value = Integer.parseInt(data);
			}
		} catch (Exception e) {
			log.error("Exception in getting notification conversation TTL information:", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return value;
	}

	public void putActiveConversation(String conversationId) {

		Jedis jedis = redisConfig.getConnection();
		DateTime dt = new DateTime(DateTimeZone.UTC);
		long currentTimeStamp = dt.getMillis() / 1000;
		int keyExpireTime = 900;
		String key = JedisEnum.ACTIVECONVERSATION + "_" + String.valueOf(conversationId);
		try {
			jedis.set(key, String.valueOf(currentTimeStamp));
			jedis.expire(key, keyExpireTime);
		} catch (Exception e) {
			log.error("Exception in creating active conversation :", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

	}

	public void createNotificationKeys(String conversationId, long userId, int value) {
		Jedis jedis = redisConfig.getConnection();
		DateTime dt = new DateTime(DateTimeZone.UTC);
		long currentTimeStamp = dt.getMillis() / 1000;
		int keyExpireTime = value * notificationTime;
		String key = JedisEnum.NOTIFICATION + "_" + String.valueOf(conversationId) + "_" + String.valueOf(userId);
		try {
			jedis.set(key, String.valueOf(currentTimeStamp));
			if (value > 0) {
				jedis.expire(key, keyExpireTime);
			}
		} catch (Exception e) {
			log.error("Exception in creating conversation notification user key:", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public void createTTLKey(String conversationId, long userId, int value) {
		Jedis jedis = redisConfig.getConnection();
		String key = JedisEnum.TTL + "_" + String.valueOf(conversationId) + "_" + String.valueOf(userId);
		try {
			jedis.set(key, String.valueOf(value));
		} catch (Exception e) {
			log.error("Exception in creating ttl key to nottified conversation :", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public void removeNotificationKeys(String conversationId, long userId) {
		Jedis jedis = redisConfig.getConnection();

		String notificationkey = JedisEnum.NOTIFICATION + "_" + String.valueOf(conversationId) + "_"
				+ String.valueOf(userId);
		String ttlKey = JedisEnum.TTL+ "_" + String.valueOf(conversationId) + "_" + String.valueOf(userId);
		try {
			jedis.del(notificationkey);
			jedis.del(ttlKey);

		} catch (Exception e) {
			log.error("Exception in removing user notificaiton keys :", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

	}

}
