package com.gmsxo.domains.db.dao;
 
import java.util.HashMap;
import java.util.Map;
 
import com.gmsxo.domains.data.User;
 
public class UserDAO extends GenericDAOHibernate<User> {
 
	private static final long serialVersionUID = 1L;

	public UserDAO() {
        super(User.class);
    }
 
    public User findUserByEmail(String email){
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("email", email);     
 
        return super.findOneResult(User.FIND_BY_EMAIL, parameters);
    }
}