local stockKey = KEYS[1]
local usersKey = KEYS[2]

local userId = ARGV[1]
local limit = tonumber(ARGV[2])

local stock = tonumber(redis.call('GET', stockKey))
if stock == nil or stock <= 0 then
    return 0
end

local current = tonumber(redis.call('HGET', usersKey, userId))
if current == nil then
    current = 0
end

if current >= limit then
    return -1
end

redis.call('DECR', stockKey)
redis.call('HINCRBY', usersKey, userId, 1)

return 1
