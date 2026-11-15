if (redis.call('GET',KEYS[1]) == ARGV[1]) then
    -- 二者一致就删除锁
    return redis.call('DEL',KEYS[1])
end
return 0