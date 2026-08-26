package com.competition.hub.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
//    业务名称
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }
//    开发规范还要一个锁的前缀
    private final String KEY_PREFIX = "lock:";
//有一个UUID的前缀最为key
    private final String ID_PREFIX = UUID.randomUUID().toString();
    @Override
    public boolean tryLock(long timeoutSec) {
//        就是有那个不空才建的：还要有返回值
        Boolean success =  stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name,ID_PREFIX+Thread.currentThread().getId(),timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
//        在这里要验证那个id对得上吗
        String myThreadID = ID_PREFIX+Thread.currentThread().getId();
//        锁中的
        String currentID  = stringRedisTemplate.opsForValue().get(KEY_PREFIX+name);
        if (myThreadID.equals(currentID)){
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }

    }
}
