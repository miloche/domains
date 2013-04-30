package com.gmsxo.domains;

public class CreateSql {

  public static void main(String[] args) {
    String[] part=new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","digit","other"};
    for (String ch:part) {
      String sql= "ALTER TABLE domain_"+ch+" DROP CONSTRAINT par_domain_"+ch+"_name_check;";
      
      //ALTER TABLE domain_a DROP CONSTRAINT par_domain_a_name_check;
          
      //"insert into domain_"+ch+" (name) select left(domain_name,position('.' in domain_name)-1) from domain where left(domain_name,1)='"+ch+"'";
      //"CREATE TABLE domain_"+ch+" (PRIMARY KEY(id), CHECK (left(name,1)='"+ch+"')) INHERITS (domain);"+
      //"CREATE INDEX domain_"+ch+"_name_idx ON domain_"+ch+" USING btree (name) WITH (fillfactor = 100);"+
      //"ALTER TABLE domain_"+ch+" OWNER TO domains;";
      
      System.out.println(sql);
    }
  }

}
