package com.lancefallon.schedulegenerator.driver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lancefallon.schedulegenerator.model.Division;
import com.lancefallon.schedulegenerator.model.Game;
import com.lancefallon.schedulegenerator.model.Team;
import com.lancefallon.schedulegenerator.model.Week;
import com.lancefallon.schedulegenerator.service.DataStore;

public class Driver {

	static DataStore dataStore;
	static int maxWeeks = 0;
	static List<Week> weeks;
	static List<Team> teams = new ArrayList<>();
	static List<Team> noMatches = new ArrayList<Team>();
	static List<Game> possibleMatchups = new ArrayList<>();
	
	public static void main(String[] args) throws IOException {
		for(int i = 0; i < 100; i++){
			boolean finished = false;
			do{
				 finished = tryMe();
			} while(!finished);
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(new File("/Users/lancefallon/Desktop/nfl_schedule_" + new Date().getTime() + ".json"), weeks);
			System.out.println("finished " + (i+1) + " schedules");
		}
	}
	
	private static boolean tryMe(){
		dataStore = DataStore.getInstance();
		teams = dataStore.getTeams();
		
//		boolean satisfied = false;
//		while(!satisfied){
			
			loadPossibleMatchups();
			
			weeks = new ArrayList<Week>();
			
			//loop through 16 weeks
			for(int i = 0; i < 16; i++){
				boolean weekSatisfied = false;
				int attempt = 0;
				while(!weekSatisfied && attempt < 1000){
					attempt++;
					Week week = new Week();
					int counter = 0;
					randomizeRemainingMatchups();
					
					//for each available matchup table (by team)
					while(week.getGames().size() <= 16 && counter < possibleMatchups.size()){
						counter = 0;
						//loop through their available games
						for(Game game : possibleMatchups){
							counter++;
							if(!isPlayingInWeek(week, game.getHomeTeam()) && !isPlayingInWeek(week, game.getAwayTeam())){
								week.getGames().add(game);
								possibleMatchups.remove(game);
								break;
							}
						}
					}
					if(week.getGames().size() == 16){
						weekSatisfied = true;
						weeks.add(week);
						if(weeks.size() > maxWeeks){
							 maxWeeks = weeks.size();
							 System.out.println("max weeks: " + maxWeeks);
						}
					} else{
						for(int j = 0; j < week.getGames().size(); j++){
							possibleMatchups.add(week.getGames().get(j));
						}
					}
				}
			}
//			if(weeks.size() == 16){
//				satisfied = true;
//			}
//		}
		
		return weeks.size() == 16;
	}
	
	private static void randomizeRemainingMatchups() {
		for(Game game : possibleMatchups){
			game.setRandomSeed(new Random().nextInt());
		}
		possibleMatchups.sort((a,b)->a.getRandomSeed() > b.getRandomSeed() ? 1 : a.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}

	private static void loadPossibleMatchups(){
		Set<Game> innerPossibleMatchups = new LinkedHashSet<>();
		for(int i = 0; i < teams.size(); i++){
			Team team = teams.get(i);
			for(int j = 0; j < teams.size(); j++){
				if(teams.get(j).equals(team))
					continue;
				
				if(canFaceOpponent(team, teams.get(j))){
					Game game = new Game();
					game.setHomeTeam(team);
					game.setAwayTeam(teams.get(j));
					game.setRandomSeed(new Random().nextInt());
					innerPossibleMatchups.add(game);
				}
			}
		}
		
		possibleMatchups = new ArrayList<>();
		for(Game game : innerPossibleMatchups){
			possibleMatchups.add(game);
		}
		
		for(int i = 0; i < possibleMatchups.size(); i++){
			for(int j = 0; j < possibleMatchups.size(); j++){
				if(possibleMatchups.get(i).getRandomSeed().equals(possibleMatchups.get(j).getRandomSeed()))
					continue;
				
				if(possibleMatchups.get(i).equals(possibleMatchups.get(j))){
					possibleMatchups.remove(possibleMatchups.get(j));
				}
			}
		}
		
		possibleMatchups.sort((a,b)->a.getRandomSeed() > b.getRandomSeed() ? 1 : a.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
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
	
	private static boolean canFaceOpponent(Team team, Team opponent) {		
		//if outside of conference
		if(checkConferenceTable(team, opponent)){
			return true;
		}
		
		//if in-conference
		return checkInConference(team, opponent);
	}
	
	private static boolean canFaceOpponent(List<Week> weeks, Team team, Team opponent) {
		List<Game> games = hasPlayedOpponent(weeks, team, opponent);
		
		//if hasn't face opponent return true (for now)
		if(games.size() == 0){
			
			//if outside of conference
			if(checkConferenceTable(team, opponent)){
				return true;
			}
			
			//if in-conference, but outside of division
			else if(checkInConference(team, opponent)){
				return true;
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
	
	public static Boolean checkInConference(Team team, Team opponent){
		if(!team.getDivision().equals(opponent.getDivision())){
			
			//check if in division matchup table
			for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
			    Division division1 = entry.getKey();
			    Division division2 = entry.getValue();
			    
			    //can play if in division matchup table
			    if((team.getDivision().equals(division1) && opponent.getDivision().equals(division2)) ||
			    		(team.getDivision().equals(division2) && opponent.getDivision().equals(division1))){
			    	return true;
			    }
			}
			
	    	//check if outside division & outside division matchup table, but play based on prev season final standings
			return conferenceStandingsMatch(team, opponent);
		}
		
		//if in own division, they can play
		return true;
	}
	
	/**
	 * return if two teams can play based on the conference matchup table
	 * @param team
	 * @param opponent
	 * @return
	 */
	public static Boolean checkConferenceTable(Team team, Team opponent){
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
		}
		return false;
	}

	/**
	 * return if a team is playing in the specified week
	 * @param week
	 * @param team
	 * @return
	 */
	public static Boolean isPlayingInWeek(Week week, Team team){
		List<Game> games = week.getGames();
		for(int i = 0; i < games.size(); i++){
			if(games.get(i).getAwayTeam().equals(team) || games.get(i).getHomeTeam().equals(team)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * return list of games where two given teams have played
	 * @param weeks
	 * @param team
	 * @param opponent
	 * @return
	 */
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
	
	/**
	 * return if in same conference and finished in same position within own division prior season
	 * @param team
	 * @param opponent
	 * @return
	 */
	public static Boolean conferenceStandingsMatch(Team team, Team opponent){
		return team.getDivision().getConference().equals(opponent.getDivision().getConference()) && team.getDivisionRank().equals(opponent.getDivisionRank());
	}

}
