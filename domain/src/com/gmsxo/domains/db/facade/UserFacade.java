package com.gmsxo.domains.db.facade;

import com.gmsxo.domains.data.User;
import com.gmsxo.domains.db.dao.UserDAO;

public class UserFacade {
	private UserDAO userDAO = new UserDAO();

	public User isValidLogin(String email, String password) {
		userDAO.createEntityManager();
		User user = userDAO.findUserByEmail(email);

		if (user == null || !user.getPassword().equals(password)) {
			return null;
		}
		userDAO.closeTransaction();
		return user;
	}
}