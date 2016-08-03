package com.lancefallon.schedulegenerator.service;

import com.lancefallon.schedulegenerator.model.Team;

public class AnalyzerService {
	
	public double getGameRating(Team team1, Team team2){
		double rating = 0.0;
		rating += team1.getPreviousSeasonWins() + team2.getPreviousSeasonWins();
		if(team1.getDivision().equals(team2.getDivision())){
			rating = rating + rating * 0.1;
		}
		rating = rating + rating * (team1.getMadePlayoffs() && team1.getMadePlayoffs() ? .4 : team1.getMadePlayoffs() || team2.getMadePlayoffs() ? .15 : 0); 
		return rating;
	}
}
