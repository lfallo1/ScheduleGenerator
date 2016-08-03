package com.lancefallon.schedulegenerator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import com.lancefallon.schedulegenerator.model.Division;
import com.lancefallon.schedulegenerator.model.Game;
import com.lancefallon.schedulegenerator.model.Team;
import com.lancefallon.schedulegenerator.model.Week;

public class ScheduleGenerationService {

	private DataStore dataStore;
	private List<Team> teams = new ArrayList<>();
	private List<Game> possibleMatchups = new ArrayList<>();
	private List<Week> weeks = new ArrayList<>();

	public ScheduleGenerationService(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public List<Week> getWeeks() {
		return weeks;
	}

	public boolean generateSchedule() {

		teams = dataStore.getTeams();

		loadPossibleMatchups();

		weeks = new ArrayList<Week>();

		// loop through 16 weeks
		for (int i = 0; i < 16; i++) {
			boolean weekSatisfied = false;
			int attempt = 0;
			while (!weekSatisfied && attempt < 1000) {
				attempt++;
				Week week = new Week();
				int counter = 0;
				randomizeRemainingMatchups();

				// for each available matchup table (by team)
				while (week.getGames().size() <= 16
						&& counter < possibleMatchups.size()) {
					counter = 0;
					// loop through their available games
					for (Game game : possibleMatchups) {
						counter++;
						if (!isPlayingInWeek(week, game.getHomeTeam())
								&& !isPlayingInWeek(week, game.getAwayTeam())) {
							week.getGames().add(game);
							possibleMatchups.remove(game);
							break;
						}
					}
				}
				if (week.getGames().size() == 16) {
					weekSatisfied = true;
					weeks.add(week);
				} else {
					for (int j = 0; j < week.getGames().size(); j++) {
						possibleMatchups.add(week.getGames().get(j));
					}
				}
			}
		}

		return weeks.size() == 16;
	}

	/**
	 * organize home / away count for teams returns how many home/away swaps
	 * were performed
	 * 
	 * @return
	 */
	public int organizeHomeAndAway() {
		int flips = 0;
		dataStore.generateTeams();
		teams = dataStore.getTeams();
		for (Team team : teams) {

			// get all games for team
			List<Game> pool = weeks
					.stream()
					.flatMap(w -> w.getGames().stream())
					.filter((g -> g.getHomeTeam().equals(team)
							|| g.getAwayTeam().equals(team)))
					.collect(Collectors.toList());

			// sort games randomly
			pool.forEach(g -> g.setRandomSeed(new Random().nextLong()));
			pool.sort((a, b) -> a.getRandomSeed() > b.getRandomSeed() ? 1 : a
					.getRandomSeed() < b.getRandomSeed() ? -1 : 0);

			// get list of team's out-of-conference games (4 games)
			List<Game> outOfConference = getOutOfConferenceGames(pool, team);
			flips += evenHomeAway(team, outOfConference);

			// get list of team's in-conference-divisional-matchup by yearly
			// cycle (4 teams)
			List<Game> inConferenceDivisionCycle = getInConferenceDivisionCycle(
					pool, team);
			flips += evenHomeAway(team, inConferenceDivisionCycle);

			// collect all in-conference-divisional-matchup by previous season
			// standings (2 games total)
			List<Game> inConferenceMatchupByRank = inConferenceMatchupsByStanding(
					pool, team);
			flips += evenHomeAway(team, inConferenceMatchupByRank);
		}

		return flips;
	}

	/**
	 * get out of conference games for a given team
	 * 
	 * @param pool
	 * @param team
	 * @return
	 */
	public List<Game> getOutOfConferenceGames(List<Game> pool, Team team) {
		return pool
				.stream()
				.filter(g -> {
					return (g.getHomeTeam().equals(team) && !g.getAwayTeam()
							.getDivision().getConference()
							.equals(team.getDivision().getConference()))
							|| (g.getAwayTeam().equals(team) && !g
									.getHomeTeam().getDivision()
									.getConference()
									.equals(team.getDivision().getConference()));
				}).collect(Collectors.toList());
	}

	/**
	 * get in conference matchups by previous year standings for a given team
	 * 
	 * @param pool
	 * @param team
	 * @return
	 */
	public List<Game> inConferenceMatchupsByStanding(List<Game> pool, Team team) {
		return pool
				.stream()
				.filter(g -> {

					// save the divisional matchup table for ease of use in
					// if-statements below
					Division divisionMatchup1 = null;
					Division divisionMatchup2 = null;
					for (Entry<Division, Division> entry : dataStore
							.getInConferenceTable().entrySet()) {
						Division division1 = entry.getKey();
						Division division2 = entry.getValue();

						// can play if in division matchup table
						if ((team.getDivision().equals(division1))
								|| team.equals(division2)) {
							divisionMatchup1 = entry.getKey();
							divisionMatchup2 = entry.getValue();
						}
					}

					// if same division then skip (probably don't even need this
					// check)
					if (g.getHomeTeam().getDivision()
							.equals(g.getAwayTeam().getDivision())) {
						return false;
					}

					// if team == homeTeam and away team is in conference
					if (g.getHomeTeam().equals(team)
							&& g.getAwayTeam().getDivision().getConference()
									.equals(team.getDivision().getConference())) {

						// if not part of the in-conference divisional table
						if (!(g.getAwayTeam().getDivision()
								.equals(divisionMatchup1) || g.getAwayTeam()
								.getDivision().equals(divisionMatchup2))) {
							return true;
						}

					}
					// if team == awayTeam and home team is in conference
					else if (g.getAwayTeam().equals(team)
							&& g.getHomeTeam().getDivision().getConference()
									.equals(team.getDivision().getConference())) {

						// if not part of the in-conference divisional table
						if (!(g.getHomeTeam().getDivision()
								.equals(divisionMatchup1) || g.getHomeTeam()
								.getDivision().equals(divisionMatchup2))) {
							return true;
						}
					}

					return false;

				}).collect(Collectors.toList());
	}

	/**
	 * get in-conference divisional opponents (yearly cycle) for a given team
	 * 
	 * @param allGames
	 * @param team
	 * @return
	 */
	public List<Game> getInConferenceDivisionCycle(List<Game> allGames,
			Team team) {
		return allGames
				.stream()
				.filter(g -> {

					// save the divisional matchup table for ease of use in
					// if-statements below
					Division divisionMatchup1 = null;
					Division divisionMatchup2 = null;
					for (Entry<Division, Division> entry : dataStore
							.getInConferenceTable().entrySet()) {
						Division division1 = entry.getKey();
						Division division2 = entry.getValue();

						// can play if in division matchup table
						if ((team.getDivision().equals(division1))
								|| team.equals(division2)) {
							divisionMatchup1 = entry.getKey();
							divisionMatchup2 = entry.getValue();
						}
					}

					// if team is at home
					if (g.getHomeTeam().equals(team)) {

						// can play if in division matchup table
						if ((g.getHomeTeam().getDivision()
								.equals(divisionMatchup1) && g.getAwayTeam()
								.getDivision().equals(divisionMatchup2))
								|| (g.getHomeTeam().getDivision()
										.equals(divisionMatchup2) && g
										.getAwayTeam().getDivision()
										.equals(divisionMatchup1))) {
							return true;
						}

					} else {
						// can play if in division matchup table
						if ((g.getAwayTeam().getDivision()
								.equals(divisionMatchup1) && g.getHomeTeam()
								.getDivision().equals(divisionMatchup2))
								|| (g.getAwayTeam().getDivision()
										.equals(divisionMatchup2) && g
										.getHomeTeam().getDivision()
										.equals(divisionMatchup1))) {
							return true;
						}
					}

					// otherwise does not qualify
					return false;

				}).collect(Collectors.toList());
	}

	public int evenHomeAway(Team team, List<Game> games) {
		int flips = 0;
		List<Game> homeGames = games.stream()
				.filter(g -> g.getHomeTeam().equals(team))
				.collect(Collectors.toList());
		List<Game> awayGames = games.stream()
				.filter(g -> g.getAwayTeam().equals(team))
				.collect(Collectors.toList());
		if (homeGames.size() > awayGames.size()) {
			int diff = homeGames.size() - awayGames.size();
			for (int i = diff / 2 - 1; i >= 0; i--) {
				Team tempHome = homeGames.get(i).getHomeTeam();
				Team tempAway = homeGames.get(i).getAwayTeam();
				homeGames.get(i).setHomeTeam(tempAway);
				homeGames.get(i).setAwayTeam(tempHome);
				flips++;
			}
		} else if (homeGames.size() < awayGames.size()) {
			int diff = awayGames.size() - homeGames.size();
			for (int i = diff / 2 - 1; i >= 0; i--) {
				Team tempHome = awayGames.get(i).getHomeTeam();
				Team tempAway = awayGames.get(i).getAwayTeam();
				awayGames.get(i).setHomeTeam(tempAway);
				awayGames.get(i).setAwayTeam(tempHome);
				flips++;
			}
		}
		return flips;
	}

	public void randomizeRemainingMatchups() {
		for (Game game : possibleMatchups) {
			game.setRandomSeed(new Random().nextLong());
		}
		possibleMatchups
				.sort((a, b) -> a.getRandomSeed() > b.getRandomSeed() ? 1 : a
						.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}

	public void loadPossibleMatchups() {
		for (int i = 0; i < teams.size(); i++) {
			Team team = teams.get(i);
			for (int j = 0; j < teams.size(); j++) {
				if (teams.get(j).equals(team))
					continue;

				if (canFaceOpponent(team, teams.get(j))) {
					Game game = new Game();
					game.setHomeTeam(team);
					game.setAwayTeam(teams.get(j));
					game.setRandomSeed(new Random().nextLong());
					possibleMatchups.add(game);
				}
			}
		}

		for (int i = 0; i < possibleMatchups.size(); i++) {
			for (int j = 0; j < possibleMatchups.size(); j++) {
				if (possibleMatchups.get(i).getRandomSeed()
						.equals(possibleMatchups.get(j).getRandomSeed()))
					continue;

				if (possibleMatchups.get(i).equals(possibleMatchups.get(j))) {
					possibleMatchups.remove(possibleMatchups.get(j));
				}
			}
		}

		possibleMatchups
				.sort((a, b) -> a.getRandomSeed() > b.getRandomSeed() ? 1 : a
						.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}

	public boolean canFaceOpponent(Team team, Team opponent) {
		// if outside of conference
		if (checkConferenceTable(team, opponent)) {
			return true;
		}

		// if in-conference
		return checkInConference(team, opponent);
	}

	public Boolean checkInConference(Team team, Team opponent) {
		if (!team.getDivision().equals(opponent.getDivision())) {

			// check if in division matchup table
			for (Entry<Division, Division> entry : dataStore
					.getInConferenceTable().entrySet()) {
				Division division1 = entry.getKey();
				Division division2 = entry.getValue();

				// can play if in division matchup table
				if ((team.getDivision().equals(division1) && opponent
						.getDivision().equals(division2))
						|| (team.getDivision().equals(division2) && opponent
								.getDivision().equals(division1))) {
					return true;
				}
			}

			// check if outside division & outside division matchup table, but
			// play based on prev season final standings
			return conferenceStandingsMatch(team, opponent);
		}

		// if in own division, they can play
		return true;
	}

	/**
	 * return if two teams can play based on the conference matchup table
	 * 
	 * @param team
	 * @param opponent
	 * @return
	 */
	public Boolean checkConferenceTable(Team team, Team opponent) {
		if (!team.getDivision().getConference()
				.equals(opponent.getDivision().getConference())) {
			for (Entry<Division, Division> entry : dataStore
					.getOutOfConferenceTable().entrySet()) {
				Division division1 = entry.getKey();
				Division division2 = entry.getValue();

				// can play if in conference matchup table
				if ((team.getDivision().equals(division1) && opponent
						.getDivision().equals(division2))
						|| (team.getDivision().equals(division2) && opponent
								.getDivision().equals(division1))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * return if a team is playing in the specified week
	 * 
	 * @param week
	 * @param team
	 * @return
	 */
	public Boolean isPlayingInWeek(Week week, Team team) {
		List<Game> games = week.getGames();
		for (int i = 0; i < games.size(); i++) {
			if (games.get(i).getAwayTeam().equals(team)
					|| games.get(i).getHomeTeam().equals(team)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * return list of games where two given teams have played
	 * 
	 * @param weeks
	 * @param team
	 * @param opponent
	 * @return
	 */
	public List<Game> hasPlayedOpponent(List<Week> weeks, Team team,
			Team opponent) {
		List<Game> gamesFacingOpponent = new ArrayList<Game>();
		for (int i = 0; i < weeks.size(); i++) {
			List<Game> games = weeks.get(i).getGames();
			for (int j = 0; j < games.size(); j++) {
				if (games.get(j).getAwayTeam().equals(team)
						&& games.get(j).getHomeTeam().equals(opponent)
						|| games.get(j).getAwayTeam().equals(opponent)
						&& games.get(j).getHomeTeam().equals(team)) {
					gamesFacingOpponent.add(games.get(j));
				}
			}
		}
		return gamesFacingOpponent;
	}

	/**
	 * return if in same conference and finished in same position within own
	 * division prior season
	 * 
	 * @param team
	 * @param opponent
	 * @return
	 */
	public Boolean conferenceStandingsMatch(Team team, Team opponent) {
		return team.getDivision().getConference()
				.equals(opponent.getDivision().getConference())
				&& team.getDivisionRank().equals(opponent.getDivisionRank());
	}
}
