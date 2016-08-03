package com.lancefallon.schedulegenerator.model;

public class Game implements Comparable<Game> {

	private Integer id;
	private Team homeTeam;
	private Team awayTeam;
	private Integer randomSeed;
//	private Integer rating;

	public Game() {
	}

	public Game(Integer id, Team homeTeam, Team awayTeam) {
		this.id = id;
		this.homeTeam = homeTeam;
		this.awayTeam = awayTeam;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Team getHomeTeam() {
		return homeTeam;
	}

	public void setHomeTeam(Team homeTeam) {
		this.homeTeam = homeTeam;
	}

	public Team getAwayTeam() {
		return awayTeam;
	}

	public void setAwayTeam(Team awayTeam) {
		this.awayTeam = awayTeam;
	}
	
	

	public Integer getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(Integer randomSeed) {
		this.randomSeed = randomSeed;
	}

//	public Integer getRating() {
//		return homeTeam.getDivisionRank() * awayTeam.getDivisionRank();
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((awayTeam == null) ? 0 : awayTeam.hashCode());
		result = prime * result + ((homeTeam == null) ? 0 : homeTeam.hashCode());
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
		Game other = (Game) obj;
		if (awayTeam == null) {
			if (other.awayTeam != null)
				return false;
		}
		//if home & away both match, it's a dup
		if(homeTeam.equals(other.getHomeTeam()) && awayTeam.equals(other.getAwayTeam())){
			return true;
		}
		else if(homeTeam.equals(other.awayTeam) && awayTeam.equals(other.homeTeam) && !homeTeam.getDivision().equals(awayTeam.getDivision())){
			//if out of division, then home / away does not matter
			return true;
		}
		
		return false;
	}

	@Override
	public int compareTo(Game game) {
		return this.compareTo(game);
	}

}
