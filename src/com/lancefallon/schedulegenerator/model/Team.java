package com.lancefallon.schedulegenerator.model;

public class Team {

	private Integer id;
	private long randomSeed;
	private String name;
	private Division division;
	private Integer divisionRank;

	public Team() {
	}

	public Team(Integer id, long randomSeed, String name, Division division, Integer divisionRank) {
		this.id = id;
		this.randomSeed = randomSeed;
		this.name = name;
		this.division = division;
		this.divisionRank = divisionRank;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Division getDivision() {
		return division;
	}

	public void setDivision(Division division) {
		this.division = division;
	}

	public long getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	

	public Integer getDivisionRank() {
		return divisionRank;
	}

	public void setDivisionRank(Integer divisionRank) {
		this.divisionRank = divisionRank;
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
		Team other = (Team) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
