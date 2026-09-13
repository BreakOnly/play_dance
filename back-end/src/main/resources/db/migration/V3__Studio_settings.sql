INSERT IGNORE INTO config(store_id,config_key,config_value,remark) VALUES
 (1,'studio_name','街舞工作室','工作室名称'),
 (1,'privacy_url','','隐私政策 HTTPS 地址'),
 (1,'terms_url','','用户协议 HTTPS 地址');

-- 仅停用未使用的抓取类商品，不删除历史商品或订单。
UPDATE goods SET status=0 WHERE category='claw';
