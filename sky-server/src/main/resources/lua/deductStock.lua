local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
local count = tonumber(ARGV[1])

if count and count > 0 and stock >= count then
    redis.call('DECRBY', KEYS[1], count)
    return 1
end

return 0
