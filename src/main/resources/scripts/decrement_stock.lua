-- KEYS[1]: Inventory key (e.g., item:5:stock)
-- ARGV[1]: Decrement quantity (usually 1)

local current_stock = redis.call('GET', KEYS[1])

if not current_stock then
    return -2 -- Key does not exist / uninitialized
end

local stock_num = tonumber(current_stock)
local decrement_by = tonumber(ARGV[1])

if stock_num >= decrement_by then
    local remaining = redis.call('DECRBY', KEYS[1], decrement_by)
    return remaining
else
    return -1 -- Insufficient inventory (Sold out)
end