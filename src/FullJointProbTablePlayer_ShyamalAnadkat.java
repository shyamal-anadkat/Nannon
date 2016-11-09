
/**
 * SAnadkat
 */
import java.util.Arrays;
import java.util.List;

public class FullJointProbTablePlayer_ShyamalAnadkat extends NannonPlayer {

	// 11 dimentions /RVs
	// the first 6 RVs are state of each of six positions on board, 
	// the 7th RV is the effect, 8th,9th,10th, 11th RV are num pieces at home and safety for X and O  
	private int[][][][][][][][][][][] fullState_win = new int[3][3][3][3][3][3][12][4][4][4][4];
	private int[][][][][][][][][][][] fullState_lost = new int[3][3][3][3][3][3][12][4][4][4][4];
	private int[] best_config = new int[12];
	private int[] bad_config = new int[12];
	private double globalBest = Integer.MIN_VALUE;
	private double globalWorst = Integer.MAX_VALUE;
	private int numWins = 1;    //Around K/10 where k is numCells
	private int numLosses = 1;  //Around K/10 
	private int numGames = 2;   //Explicit counter 

	//A good way to create your players is to edit these methods.  See PlayNannon.java for more details.
	@Override
	public String getPlayerName() { return "Shyamal's FullJointProbTable Player"; }
	// Constructors.
	public FullJointProbTablePlayer_ShyamalAnadkat() { 
		initialize();
	}
	public FullJointProbTablePlayer_ShyamalAnadkat(NannonGameBoard gameBoard) {
		super(gameBoard);
		initialize();
	}

	private void initialize() {
		// Put things here needed for instance creation.
		// avoid having prob = 0 so init both win and lost fullstate with 1s. 
		for (int a = 0; a < 3; a++) {
			for (int b = 0; b < 3; b++) {
				for (int c = 0; c < 3; c++) {
					for (int d = 0; d < 3; d++) {
						for (int e = 0; e < 3; e++) {
							for (int f = 0; f < 3; f++) {
								for (int g = 0; g < 12; g++) {
									for (int h = 0; h < 4; h++) {
										for (int i = 0; i < 4; i++) {
											for (int j = 0; j < 4; j++) {
												for (int k = 0; k < 4; k++) {
													fullState_win[a][b][c][d][e][f][g][h][i][j][k] = 1; 
													fullState_lost[a][b][c][d][e][f][g][h][i][j][k] = 1; 
												}
											}

										}
									}
								}
							}
						}
					}
				}
			}
		}
		//init 
		for(int i = 0; i < best_config.length; i++) {
			best_config[i] = Integer.MIN_VALUE;
		}
		//init 
		for(int i = 0; i < best_config.length; i++) {
			best_config[i] = Integer.MAX_VALUE;
		}
	}
	@SuppressWarnings("unused") // This prevents a warning from the "if (false)" below.
	@Override
	public List<Integer> chooseMove(int[] boardConfiguration, List<List<Integer>> legalMoves) {
		// Below is some code you might want to use in your solution.
		//      (a) converts to zero-based counting for the cell locations
		//      (b) converts NannonGameBoard.movingFromHOME and NannonGameBoard.movingToSAFE to NannonGameBoard.cellsOnBoard,
		//          (so you could then make arrays with dimension NannonGameBoard.cellsOnBoard+1)
		//      (c) gets the current and next board configurations.
		List<Integer> chosenMove = null;
		double best_prob = Integer.MIN_VALUE; 
		int numLegalMoves = legalMoves.size();

		if (legalMoves != null) 
			for (List<Integer> move : legalMoves) { 

				int fromCountingFromOne = move.get(0);  // Convert below to an internal count-from-zero system.
				int toCountingFromOne = move.get(1);			
				int effect = move.get(2);  // See ManageMoveEffects.java for the possible values that can appear here.	

				// Note we use 0 for both 'from' and 'to' because one can never move FROM SAFETY or TO HOME, so we save a memory cell.
				int from = (fromCountingFromOne == NannonGameBoard.movingFromHOME ? 0 : fromCountingFromOne);
				int to   = (toCountingFromOne   == NannonGameBoard.movingToSAFETY ? 0 : toCountingFromOne);

				// The 'effect' of move is encoded in these four booleans:
				boolean hitOpponent = ManageMoveEffects.isaHit(effect);  // Did this move 'land' on an opponent (sending it back to HOME)?
				boolean brokeMyPrime = ManageMoveEffects.breaksPrime(effect);  // A 'prime' is when two pieces from the same player are adjacent on the board;
				// an opponent can NOT land on pieces that are 'prime' - so breaking up a prime of 
				// might be a bad idea.
				boolean extendsPrimeOfMine = ManageMoveEffects.extendsPrime(effect);  // Did this move lengthen (i.e., extend) an existing prime?
				boolean createsPrimeOfMine = ManageMoveEffects.createsPrime(effect);  // Did this move CREATE a NEW prime? (A move cannot both extend and create a prime.)

				int[] resultingBoard = gameBoard.getNextBoardConfiguration(boardConfiguration, move);  // You might choose NOT to use this - see updateStatistics().
				int atHomeX = 0 , atSafeX = 0, dieVal = 0, atHomeO = 0, atSafeO = 0; 
				switch(NannonGameBoard.getWhoseTurn(resultingBoard)) {
				case 2: //O's turn 
					atHomeO = resultingBoard[2];
					atSafeO = resultingBoard[4]; //4
					dieVal = resultingBoard[6];
					break;
				case 1: //X's turn
					atHomeX = resultingBoard[1];
					atSafeX= resultingBoard[3];
					dieVal = resultingBoard[5];
					break;
				}

				double numWins = (double) fullState_win[resultingBoard[7]][resultingBoard[8]]
						[resultingBoard[9]][resultingBoard[10]][resultingBoard[11]]
								[resultingBoard[12]][effect][atHomeX][atSafeX][atHomeO][atSafeO];
				double numLosses = (double) fullState_lost[resultingBoard[7]][resultingBoard[8]]
						[resultingBoard[9]][resultingBoard[10]][resultingBoard[11]]
								[resultingBoard[12]][effect][atHomeX][atSafeX][atHomeO][atSafeO];
				double probOfWin = numWins/(double) (numWins+numLosses);
				double probOfLoss = numLosses/(double) (numWins+numLosses);

				double bestRatio = (double) probOfWin / (double) probOfLoss;
				
				//update learned model statistics 
				if(bestRatio >= best_prob) {
					best_prob = bestRatio; 
					chosenMove = move; 

				} 
				if(bestRatio < globalWorst) {
					globalWorst = bestRatio;
					bad_config[0] = resultingBoard[7];
					bad_config[1] = resultingBoard[8];
					bad_config[2] = resultingBoard[9];
					bad_config[3] = resultingBoard[10];
					bad_config[4] = resultingBoard[11];
					bad_config[5] = resultingBoard[12];
					bad_config[6] = effect;
					bad_config[7] = atHomeX;
					bad_config[8] = atSafeX;
					bad_config[9] = atHomeO;
					bad_config[10] = atSafeO;
					bad_config[11] = dieVal;
				} 
				if (bestRatio > globalBest) {
					globalBest = bestRatio; 
					best_config[0] = resultingBoard[7];
					best_config[1] = resultingBoard[8];
					best_config[2] = resultingBoard[9];
					best_config[3] = resultingBoard[10];
					best_config[4] = resultingBoard[11];
					best_config[5] = resultingBoard[12];
					best_config[6] = effect;
					best_config[7] = atHomeX;
					best_config[8] = atSafeX;
					best_config[9] = atHomeO;
					best_config[10] = atSafeO;
					best_config[11] = dieVal;
				}

			}
		/* Here is what is in a board configuration vector.  There are also accessor functions in NannonGameBoard.java (starts at or around line 60).
			   	boardConfiguration[0] = whoseTurn;        // Ignore, since it is OUR TURN when we play, by definition. (But needed to compute getNextBoardConfiguration.)
        		boardConfiguration[1] = homePieces_playerX; 
        		boardConfiguration[2] = homePieces_playerO;
        		boardConfiguration[3] = safePieces_playerX;
        		boardConfiguration[4] = safePieces_playerO;
        		boardConfiguration[5] = die_playerX;      // I added these early on, but never used them.
        		boardConfiguration[6] = die_playerO;      // Probably can be ignored since get the number of legal moves, which is more meaningful.
        		cells 7 to (6 + NannonGameBoard.cellsOnBoard) record what is on the board at each 'cell' (ie, board location).
        					- one of NannonGameBoard.playerX, NannonGameBoard.playerO, or NannonGameBoard.empty.
		 */
		return chosenMove == null ? Utils.chooseRandomElementFromThisList(legalMoves):chosenMove;
	}

	@SuppressWarnings("unused") // This prevents a warning from the "if (false)" below.
	@Override
	public void updateStatistics(boolean             didIwinThisGame, 
			List<int[]>         allBoardConfigurationsThisGameForPlayer,
			List<Integer>       allCountsOfPossibleMovesForPlayer,
			List<List<Integer>> allMovesThisGameForPlayer) {
		int atHomeX = 0 , atSafeX = 0, dieVal = 0, atHomeO = 0, atSafeO = 0; 
		// Do nothing with these in the random player (but hints are here for use in your players).	

		// However, here are the beginnings of what you might want to do in your solution (see comments in 'chooseMove' as well).
		// <------------ Be sure to remove this 'false' *********************************************************************
		int numberOfMyMovesThisGame = allBoardConfigurationsThisGameForPlayer.size();	

		for (int myMove = 0; myMove < numberOfMyMovesThisGame; myMove++) {
			int[]         currentBoard        = allBoardConfigurationsThisGameForPlayer.get(myMove);
			int           numberPossibleMoves = allCountsOfPossibleMovesForPlayer.get(myMove);
			List<Integer> moveChosen          = allMovesThisGameForPlayer.get(myMove);
			int[]         resultingBoard      = (numberPossibleMoves < 1 ? currentBoard // No move possible, so board is unchanged.
					: gameBoard.getNextBoardConfiguration(currentBoard, moveChosen));
			// You should compute the statistics needed for a Bayes Net for any of these problem formulations:
			//
			//     prob(win | currentBoard and chosenMove and chosenMove's Effects)  <--- this is what I (Jude) did, but mainly because at that point I had not yet written getNextBoardConfiguration()
			//     prob(win | resultingBoard and chosenMove's Effects)               <--- condition on the board produced and also on the important changes from the prev board
			//     
			//     prob(win | currentBoard and chosenMove)                           <--- if we ignore 'chosenMove's Effects' we would be more in the spirit of a State Board Evaluator (SBE)
			//     prob(win | resultingBoard)                                        <--- but it seems helpful to know something about the impact of the chosen move (ie, in the first two options)
			//
			//     prob(win | currentBoard)                                          <--- if you estimate this, be sure when CHOOSING moves you apply to the NEXT boards (since when choosing moves, one needs to score each legal move).
			//
			if (numberPossibleMoves < 1) { continue; } // If NO moves possible, nothing to learn from (it is up to you if you want to learn for cases where there is a FORCED move, ie only one possible move).
			// Convert to our internal count-from-zero system.
			// A move is a list of three integers.  Their meanings should be clear from the variable names below.
			int fromCountingFromOne = moveChosen.get(0);  // Convert below to an internal count-from-zero system.
			int toCountingFromOne = moveChosen.get(1);
			int effect = moveChosen.get(2);  // See ManageMoveEffects.java for the possible values that can appear here. Also see the four booleans below.

			// Note we use 0 for both 'from' and 'to' because one can never move FROM SAFETY or TO HOME, so we save a memory cell.
			int from = (fromCountingFromOne == NannonGameBoard.movingFromHOME ? 0 : fromCountingFromOne);
			int to   = (toCountingFromOne   == NannonGameBoard.movingToSAFETY ? 0 : toCountingFromOne);

			// The 'effect' of move is encoded in these four booleans:
			boolean        hitOpponent = ManageMoveEffects.isaHit(effect); // Explained in chooseMove() above.
			boolean       brokeMyPrime = ManageMoveEffects.breaksPrime( effect);
			boolean extendsPrimeOfMine = ManageMoveEffects.extendsPrime(effect);
			boolean createsPrimeOfMine = ManageMoveEffects.createsPrime(effect);

			switch(resultingBoard[0]) {
			case 2: //O's turn 
				atHomeO = resultingBoard[2];
				atSafeO = resultingBoard[4];
				dieVal = resultingBoard[6];
				break;
			case 1: //X's turn
				atHomeX = resultingBoard[1];
				atSafeX = resultingBoard[3];
				dieVal = resultingBoard[5];
				break;
			}
			//updating table for winning and losing moves 
			if(didIwinThisGame) {
				fullState_win[resultingBoard[7]][resultingBoard[8]]
						[resultingBoard[9]][resultingBoard[10]][resultingBoard[11]]
								[resultingBoard[12]][effect][atHomeX][atSafeX][atHomeO][atSafeO]++;
			} else{
				fullState_lost[resultingBoard[7]][resultingBoard[8]]
						[resultingBoard[9]][resultingBoard[10]][resultingBoard[11]]
								[resultingBoard[12]][effect][atHomeX][atSafeX][atHomeO][atSafeO]++;
			}
		}
	}
	@Override
	public void reportLearnedModel() { // You can add some code here that reports what was learned, 
		//eg the most important feature for WIN and for LOSS.  And/or all the weights on your features.
		Utils.println("\n--------------------------------------------------------------------------------------");
		Utils.println("\n"+getPlayerName()+" learned and reporting a full joint probablity reasonor for Nannon.");
		Utils.print("\nBest Values for Position 1 through 6 on board: "
				+best_config[0]+","+best_config[1]+","+best_config[2]+","+best_config[3]+","+best_config[4]+","+best_config[5]);
		Utils.print("\nBest Value for effect: "+best_config[6]);
		Utils.print("\nBest value for pieces at Home for X: "+best_config[7] );
		Utils.print("\nBest value for pieces at Safe for X: "+best_config[8] );
		Utils.print("\nBest value for pieces at Home for O: "+best_config[9] );
		Utils.print("\nBest value for pieces at Safe for O: "+best_config[10] );
		Utils.print("\nBest Die Value: "+best_config[11]);
		Utils.print("\nBad Values for Position 1 through 6 on board: "
				+bad_config[0]+","+bad_config[1]+","+bad_config[2]+","+bad_config[3]+","+bad_config[4]+","+bad_config[5]);
		Utils.print("\nBad Value for effect: "+best_config[6]);
		Utils.print("\nBad value for pieces at Home for X: "+bad_config[7] );
		Utils.print("\nBad value for pieces at Safe for X: "+bad_config[8] );
		Utils.print("\nBad value for pieces at Home for O: "+bad_config[9] );
		Utils.print("\nBad value for pieces at Safe for O: "+bad_config[10] );
		Utils.print("\nBad Die Value: "+bad_config[11]);
		Utils.println("\n--------------------------------------------------------------------------------------");
		//tried with die value but got 64% and with effect got around 67%
	}
}
