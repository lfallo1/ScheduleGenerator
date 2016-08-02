package com.lancefallon.schedulegenerator.driver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lancefallon.schedulegenerator.model.Division;
import com.lancefallon.schedulegenerator.model.Game;
import com.lancefallon.schedulegenerator.model.Team;
import com.lancefallon.schedulegenerator.model.Week;
import com.lancefallon.schedulegenerator.service.DataStore;

public class Driver {

	static DataStore dataStore;

	static List<Week> weeks = new ArrayList<Week>();
	static List<Team> teams = new ArrayList<>();
	static List<Team> noMatches = new ArrayList<Team>();
	
	public static void main(String[] args) throws IOException {
		
		dataStore = DataStore.getInstance();
		
		//for each week
		for (int i = 0; i < 16; i++) {
			
			Week week = new Week();
			weeks.add(week);
			
			boolean satisfied = false;
			while(!satisfied){
				noMatches = new ArrayList<Team>();
				
				//get list of teams in random order of teams
				dataStore.generateTeams();
				teams = dataStore.getTeams();
				generateWeeklyGames(week, teams, false);
				if(week.getGames().size() == teams.size() / 2){
					noMatches = new ArrayList<Team>();
					satisfied = true;
				} else{
					if(noMatches.size() > 0){
						generateWeeklyGames(week, noMatches, true);
						if(week.getGames().size() == teams.size() / 2){
							noMatches = new ArrayList<Team>();
							satisfied = true;
						}
					}
				}
				week = new Week();
			};
		}
		System.out.println("Finished...");
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File("/Users/lancefallon/Desktop/nfl_schedule_" + new Date().getTime() + ".json"), weeks);
	}
	
	private static void generateWeeklyGames(Week week, List<Team> teamsParam, boolean searchingNoMatches){
		
		//loop through each team
		for(int j = 0; j < teamsParam.size(); j++){
			Team team = teamsParam.get(j);
			Game game = new Game();
			
			if(isPlayingInWeek(week, team)){
				continue;
			}
			
			boolean found = false;
			
			//loop through all possible opponents
			for(int k = 0; k < teamsParam.size(); k++){
				if(found){
					break;
				}
				//if same team, then skip
				if(teamsParam.get(k).equals(team)){
					continue;
				}
				
				//check if can face opponent
				if(!isPlayingInWeek(week, teamsParam.get(k)) && canFaceOpponent(weeks, team, teamsParam.get(k))){
					game.setId((int) new Date().getTime());
					game.setHomeTeam(team);
					game.setAwayTeam(teamsParam.get(k));
					week.getGames().add(game);
					found = true;
				}
				else if(!isPlayingInWeek(week, teamsParam.get(k)) && canFaceOpponent(weeks, teamsParam.get(k), team)){
					game.setId((int) new Date().getTime());
					game.setHomeTeam(teamsParam.get(k));
					game.setAwayTeam(team);
					week.getGames().add(game);
					found = true;
				}
			}
			
			if(!found && !searchingNoMatches){
				noMatches.add(teamsParam.get(j));
			}
		}
	}
	
	private static boolean canFaceOpponent(List<Week> weeks, Team team, Team opponent) {
		List<Game> games = hasPlayedOpponent(weeks, team, opponent);
		
		//if hasn't face opponent return true (for now)
		if(games.size() == 0){
			
			//if outside of conference
			if(!team.getDivision().getConference().equals(opponent.getDivision().getConference())){
				for(Entry<Division, Division> entry : dataStore.getOutOfConferenceTable().entrySet()) {
				    Division division1 = entry.getKey();
				    Division division2 = entry.getValue();
				    
				    //can play if in conference matchup table
				    if((team.getDivision().equals(division1) && opponent.getDivision().equals(division2)) ||
				    		(team.getDivision().equals(division2) && opponent.getDivision().equals(division1))){
				    	return true;
				    }
				}
				return false;
			}
			
			//if outside of division
			else if(!team.getDivision().equals(opponent.getDivision())){
				for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
				    Division division1 = entry.getKey();
				    Division division2 = entry.getValue();
				    
				    //can play if in division matchup table
				    if((team.getDivision().equals(division1) && opponent.getDivision().equals(division2)) ||
				    		(team.getDivision().equals(division2) && opponent.getDivision().equals(division1))){
				    	return true;
				    }
				}
				
		    	//check if in conference, but outside division matchup table
				if(!hasPlayedOpponentInDivision(team, opponent.getDivision())){
					return true;
				}
				
				return false;
			}
			else{
				//otherwise, it's in division and they have not played, so just add
				return true;	
			}
		}
		
		//if already played & not in same division, then they cannot play again
		if(!team.getDivision().equals(opponent.getDivision())){
			return false;
		}
		
		//if already played twice, then cannot play again
		if(games.size() == 2){
			return false;
		}
		
		//if played once at home already, they cannot play at home again
		if(games.get(0).getHomeTeam().equals(team)){
			return false;
		}
		
		return true;
	}

	public static Boolean isPlayingInWeek(Week week, Team team){
		List<Game> games = week.getGames();
		for(int i = 0; i < games.size(); i++){
			if(games.get(i).getAwayTeam().equals(team) || games.get(i).getHomeTeam().equals(team)){
				return true;
			}
		}
		return false;
	}
	
	public static List<Game> hasPlayedOpponent(List<Week> weeks, Team team, Team opponent){
		List<Game> gamesFacingOpponent = new ArrayList<Game>();
		for (int i = 0; i < weeks.size(); i++) {
			List<Game> games = weeks.get(i).getGames();
			for(int j = 0; j < games.size(); j++){
				if(games.get(j).getAwayTeam().equals(team) && games.get(j).getHomeTeam().equals(opponent) ||
					games.get(j).getAwayTeam().equals(opponent) && games.get(j).getHomeTeam().equals(team)){
					gamesFacingOpponent.add(games.get(j));
				}
			}
		}
		return gamesFacingOpponent;
	}
	
	public static Boolean hasPlayedOpponentInDivision(Team team, Division opponentDivision){
		for (int i = 0; i < weeks.size(); i++) {
			List<Game> games = weeks.get(i).getGames();
			for(int j = 0; j < games.size(); j++){
				if(games.get(j).getAwayTeam().equals(team) && games.get(j).getHomeTeam().getDivision().equals(opponentDivision) ||
					games.get(j).getAwayTeam().getDivision().equals(opponentDivision) && games.get(j).getHomeTeam().equals(team)){
					return true;
				}
			}
		}
		return false;
	}

}
