package com.lancefallon.schedulegenerator.model;

import java.util.ArrayList;
import java.util.List;

public class Week {

	private Integer id;
	private List<Team> byeTeams = new ArrayList<Team>();
	private List<Game> games = new ArrayList<Game>();

	public Week() {
	}

	public Week(Integer id, List<Team> byeTeams, List<Game> games) {
		this.id = id;
		this.byeTeams = byeTeams;
		this.games = games;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Team> getByeTeams() {
		return byeTeams;
	}

	public void setByeTeams(List<Team> byeTeams) {
		this.byeTeams = byeTeams;
	}

	public List<Game> getGames() {
		return games;
	}

	public void setGames(List<Game> games) {
		this.games = games;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Week other = (Week) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
