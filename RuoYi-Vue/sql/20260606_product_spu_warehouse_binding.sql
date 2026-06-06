-- 商城商品 SPU 发货仓库绑定表
-- 仓库主数据复用 warehouse；本表只保存商品当前允许发货的仓库快照，不承载库存数量。

create table if not exists product_spu_warehouse (
    id bigint not null auto_increment comment '主键',
    spu_id bigint not null comment '商品SPU ID',
    warehouse_id bigint not null comment '仓库ID',
    warehouse_code varchar(64) not null comment '仓库编码快照',
    warehouse_name varchar(200) not null comment '仓库名称快照',
    warehouse_kind varchar(32) not null comment '仓库类型快照：official/third_party',
    settlement_currency varchar(16) not null comment '仓库结算币种快照，也是SKU币种来源',
    seller_id bigint not null comment '商品卖家ID快照',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime default null comment '创建时间',
    primary key (id),
    unique key uk_product_spu_warehouse (spu_id, warehouse_id),
    key idx_product_spu_warehouse_warehouse (warehouse_id),
    key idx_product_spu_warehouse_seller (seller_id),
    key idx_product_spu_warehouse_currency (settlement_currency),
    constraint fk_product_spu_warehouse_spu foreign key (spu_id) references product_spu (spu_id),
    constraint fk_product_spu_warehouse_warehouse foreign key (warehouse_id) references warehouse (warehouse_id),
    constraint fk_product_spu_warehouse_seller foreign key (seller_id) references seller (seller_id)
) engine=InnoDB default charset=utf8mb4 comment='商品SPU发货仓库绑定';
