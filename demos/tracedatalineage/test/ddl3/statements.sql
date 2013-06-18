create table product_tlb(
base_cow_id int NOT NULL,
column2 int NOT NULL,
std_tst_id int NOT NULL
);

create table product_loc_tb(
bsecowid int NOT NULL,
column2b  int NOT NULL,
tstid int NOT NULL
);

UPDATE product_tlb 
SET base_cow_id = b.bsecowid,
	 column2 = b.column2b
FROM product_tlb a, product_loc_tb b 
WHERE a.std_tst_id = b.tstid 
AND b.bsecowid > 1