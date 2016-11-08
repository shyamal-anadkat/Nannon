/**
 * Copyrighted 2013 by Jude Shavlik.  Maybe be freely used for non-profit educational purposes.
 */

/*
 * A player that simply random chooses from among the possible moves.
 * 
 * NOTE: I (Jude) recommend you COPY THIS TO YOUR PLAYER (NannonPlayer_yourLoginName.java) AND THEN EDIT IN THAT FILE.
 * 
 *       Be sure to change "Random-Move Player" in getPlayerName() to something unique to you!
 */

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author SAnadkat
 *
 */
public class NaiveBayesNetPlayer_ShyamalAnadkat extends NannonPlayer {

	/*******DECLARE RANDOM VARIABLES**********/
	// Reference : lecture ppt (Jude Shavlik - 2016) 
	private static int boardSize = NannonGameBoard.getCellsOnBoard(); //6
	private static int pieces = NannonGameBoard.getPiecesPerPlayer(); //3
	int homeX_win[] = new int[pieces+1];   //holds p(homeX=? | win) 
	int homeX_lose[] = new int[pieces+1];  //holds p(homeX=? | !win)
	int safeX_win[] = new int[pieces+1];   //holds p(safeX=? | win)
	int safeX_lose[] = new int[pieces+1];  //holds p(safeX=? | !win)
	int effects_win[] = new int[12];       //holds p(effect=? | win)
	int effects_loss[] = new int[12];      //holds p(effect=? |!win)
	int homeO_win[] = new int[pieces+1];   //holds p(homeO=? | win) 
	int homeO_lose[] = new int[pieces+1];  //holds p(homeO=? | !win)
	int safeO_win[] = new int[pieces+1];   //holds p(safeO=? | !win)
	int safeO_lose[] = new int[pieces+1];  //holds p(safeO=? | !win)

	//holds p( state of pieces on board ) | win and 
	//      p( state of pieces on board ) | !win and 
	int board_win[][] = new int[boardSize][3]; //3 as X,O or blank 
	int board_lose[][] = new int[boardSize][3];  

	//best and worst configs for learned models 
	int best_config[] = new int[6];
	int bad_config[] = new int[6];
	private double globalBestHomeX, globalBestHomeO, globalBestSafeX, globalBestSafeO, globalBestEffect = Integer.MIN_VALUE;
	private double globalWorstHomeX, globalWorstHomeO, globalWorstSafeX, globalWorstSafeO, globalWorstEffect = Integer.MAX_VALUE;

	int winCnt = 1 ; //m-estimates 
	int lossCnt = 1; 

	@Override
	public String getPlayerName() { return "Shyamal's Naive Bayes Player"; }

	// Constructors.
	public NaiveBayesNetPlayer_ShyamalAnadkat() {
		initialize();

	}
	public NaiveBayesNetPlayer_ShyamalAnadkat(NannonGameBoard gameBoard) {
		super(gameBoard);
		initialize();
	}

	/**
	 * Initializing probs to 1 - m estimates
	 */
	private void initialize() {
		// Put things here needed for instance creation.
		Arrays.fill(homeX_win, 1);
		Arrays.fill(homeX_lose, 1);
		Arrays.fill(safeX_win, 1);
		Arrays.fill(safeX_lose, 1);
		Arrays.fill(homeO_win, 1);
		Arrays.fill(homeO_lose, 1);
		Arrays.fill(safeO_win, 1);
		Arrays.fill(safeO_lose, 1);
		Arrays.fill(effects_win, 1);
		Arrays.fill(effects_loss, 1);

		for(int i = 0; i < board_win.length; i++) {
			for(int j = 0; j < board_win[0].length; j++){
				board_win[i][j] = 1; 
				board_lose[i][j] = 1; 
			}
		}
	}

	@SuppressWarnings("unused") // This prevents a warning from the "if (false)" below.
	@Override
	public List<Integer> chooseMove(int[] boardConfiguration, List<List<Integer>> legalMoves) {

		double bestProb = Integer.MIN_VALUE;

		// Below is some code you might want to use in your solution.
		//      (a) converts to zero-based counting for the cell locations
		//      (b) converts NannonGameBoard.movingFromHOME and NannonGameBoard.movingToSAFE to NannonGameBoard.cellsOnBoard,
		//          (so you could then make arrays with dimension NannonGameBoard.cellsOnBoard+1)
		//      (c) gets the current and next board configurations.
		List<Integer> chosenMove = null; 
		if (legalMoves != null) 
			for (List<Integer> move : legalMoves) { // <----- be sure to drop the "false &&" !

				int fromCountingFromOne    = move.get(0);  // Convert below to an internal count-from-zero system.
				int   toCountingFromOne    = move.get(1);			
				int                 effect = move.get(2);  // See ManageMoveEffects.java for the possible values that can appear here.	

				// Note we use 0 for both 'from' and 'to' because one can never move FROM SAFETY or TO HOME, so we save a memory cell.
				int from = (fromCountingFromOne == NannonGameBoard.movingFromHOME ? 0 : fromCountingFromOne);
				int to   = (toCountingFromOne   == NannonGameBoard.movingToSAFETY ? 0 : toCountingFromOne);

				// The 'effect' of move is encoded in these four booleans:
				boolean        hitOpponent = ManageMoveEffects.isaHit(      effect);  // Did this move 'land' on an opponent (sending it back to HOME)?
				boolean       brokeMyPrime = ManageMoveEffects.breaksPrime( effect);  // A 'prime' is when two pieces from the same player are adjacent on the board;
				// an opponent can NOT land on pieces that are 'prime' - so breaking up a prime of 
				// might be a bad idea.
				boolean extendsPrimeOfMine = ManageMoveEffects.extendsPrime(effect);  // Did this move lengthen (i.e., extend) an existing prime?
				boolean createsPrimeOfMine = ManageMoveEffects.createsPrime(effect);  // Did this move CREATE a NEW prime? (A move cannot both extend and create a prime.)

				// Note that you can compute other effects than the four above (but you need to do it from the info in boardConfiguration, resultingBoard, and move).
				// See comments in updateStatistics() regarding how to use these.
				int[] resultingBoard = gameBoard.getNextBoardConfiguration(boardConfiguration, move);  // You might choose NOT to use this - see updateStatistics().

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

				// P(Random Variable given Win) conditional probablities 
				double homeXGivenWin = (double) homeX_win[resultingBoard[1]] /(double) winCnt;
				double safeXGivenWin = (double) safeX_win[resultingBoard[3]]/ (double) winCnt;
				double homeOGivenWin = (double) homeO_win[resultingBoard[2]] / (double) winCnt;
				double safeOGivenWin = (double) safeO_win[resultingBoard[4]]/ (double) winCnt;

				// P(random variable given Loss) conditional probs 
				double homeXGivenLoss = (double) homeX_lose[resultingBoard[1]] /(double) lossCnt;
				double safeXGivenLoss = (double) safeX_lose[resultingBoard[3]]/ (double) lossCnt;
				double homeOGivenLoss = (double) homeO_lose[resultingBoard[2]] / (double) lossCnt;
				double safeOGivenLoss = (double) safeO_lose[resultingBoard[4]]/ (double) lossCnt;
				
				//similarly for effect random variable
				double effectGivenWin = (double) effects_win[effect]/(double) winCnt; 
				double effectGivenLoss = (double) effects_loss[effect] /(double) lossCnt; 
				
				//probWin and probLoss calculation
				double probWin = (double)winCnt / (double) (winCnt + lossCnt);
				double probLoss = (double)lossCnt / (double) (winCnt + lossCnt);

				//**********************LEARNED MODEL STATS ********************//
				double homeXRatio = homeXGivenWin / (double) homeXGivenLoss;
				double safeXRatio = safeXGivenWin / (double) safeXGivenLoss;
				double homeORatio = homeOGivenWin / (double) homeOGivenLoss;
				double safeORatio = safeOGivenWin / (double) safeOGivenLoss;
				double effectRatio = effectGivenWin / (double) effectGivenLoss; 

				// updating stats for the learned model outputting best ratios for the feautures or each random variable 
				globalBestHomeX = Double.max(globalBestHomeX, homeXRatio);
				globalBestHomeO = Double.max(globalBestHomeO, homeORatio);
				globalBestSafeX = Double.max(globalBestSafeX, safeXRatio);
				globalBestSafeO = Double.max(globalBestSafeO, safeORatio);
				globalBestEffect =Double.max(globalBestEffect, effectRatio);

				globalWorstHomeX = Double.min(globalWorstHomeX, homeXRatio);
				globalWorstHomeO = Double.min(globalWorstHomeO, homeORatio);
				globalWorstSafeX = Double.min(globalWorstSafeX, safeXRatio);
				globalWorstSafeO = Double.min(globalWorstSafeO, safeORatio);
				globalWorstEffect =Double.min(globalWorstEffect, effectRatio);

				//assuming independence from Naive Bayes so we simply multiply them 
				double bestWinProb = (homeXGivenWin * safeXGivenWin * homeOGivenWin* safeOGivenWin * effectGivenWin * probWin)/ 
						(double)     (homeXGivenLoss * safeXGivenLoss * homeOGivenLoss* safeOGivenLoss * effectGivenLoss* probLoss);

				//determing next best move 
				if (bestWinProb > bestProb) {
					bestProb = bestWinProb; 
					chosenMove = move; 
				}
			}
		return chosenMove == null ? Utils.chooseRandomElementFromThisList(legalMoves): chosenMove; 
	}

	@SuppressWarnings("unused") // This prevents a warning from the "if (false)" below.
	@Override
	public void updateStatistics(boolean             didIwinThisGame, 
			List<int[]>         allBoardConfigurationsThisGameForPlayer,
			List<Integer>       allCountsOfPossibleMovesForPlayer,
			List<List<Integer>> allMovesThisGameForPlayer) {

		// Do nothing with these in the random player (but hints are here for use in your players).	
		// However, here are the beginnings of what you might want to do in your solution (see comments in 'chooseMove' as well).
		// <------------ Be sure to remove this 'false' *********************************************************************
		int numberOfMyMovesThisGame = allBoardConfigurationsThisGameForPlayer.size();	
		if (didIwinThisGame) {
			winCnt++;
		} else {
			lossCnt++;
		}
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

			if (numberPossibleMoves < 1) { continue; } // If NO moves possible, nothing to learn from (it is up to you if you want to learn for cases where there is a FORCED move, ie only one possible move).

			// Convert to our internal count-from-zero system.
			// A move is a list of three integers.  Their meanings should be clear from the variable names below.
			int fromCountingFromOne = moveChosen.get(0);  // Convert below to an internal count-from-zero system.
			int   toCountingFromOne = moveChosen.get(1);
			int              effect = moveChosen.get(2);  // See ManageMoveEffects.java for the possible values that can appear here. Also see the four booleans below.

			// Note we use 0 for both 'from' and 'to' because one can never move FROM SAFETY or TO HOME, so we save a memory cell.
			int from = (fromCountingFromOne == NannonGameBoard.movingFromHOME ? 0 : fromCountingFromOne);
			int to   = (toCountingFromOne   == NannonGameBoard.movingToSAFETY ? 0 : toCountingFromOne);

			// The 'effect' of move is encoded in these four booleans:
			boolean hitOpponent = ManageMoveEffects.isaHit(      effect); // Explained in chooseMove() above.
			boolean brokeMyPrime = ManageMoveEffects.breaksPrime( effect);
			boolean extendsPrimeOfMine = ManageMoveEffects.extendsPrime(effect);
			boolean createsPrimeOfMine = ManageMoveEffects.createsPrime(effect);

			//updating counter for wins and losses 
			if(didIwinThisGame) {
				homeX_win[resultingBoard[1]]++;
				safeX_win[resultingBoard[3]]++;
				homeO_win[resultingBoard[2]]++;
				safeO_win[resultingBoard[4]]++;
				effects_win[effect]++;
			} else {
				homeX_lose[resultingBoard[1]]++;
				safeX_lose[resultingBoard[3]]++;
				homeO_lose[resultingBoard[2]]++;
				safeO_lose[resultingBoard[4]]++;
				effects_loss[effect]++;
			}
		}
	}
	@Override
	public void reportLearnedModel() { // You can add some code here that reports what was learned, eg the most important feature for WIN and for LOSS.  And/or all the weights on your features.
		Utils.println("\n-------------------------------------------------");
		Utils.println(getPlayerName() + "learning model !!");		
		Utils.print("\nBest Winning Ratio for effect: "+ globalBestEffect);
		Utils.print("\nBest Winning Ratio for pieces at Home for X: "+globalBestHomeX );
		Utils.print("\nBest Winning Ratio for pieces at Safe for X: "+globalBestSafeX );
		Utils.print("\nBest Winnning Ratio for pieces at Home for O: "+globalBestHomeO );
		Utils.print("\nBest Winning Ratio for pieces at Safe for O: "+ globalBestSafeO);

		Utils.print("\nWorst Ratio for effect: "+ globalWorstEffect);
		Utils.print("\nWorst Ratio for pieces at Home for X: "+globalWorstHomeX );
		Utils.print("\nWorst Ratio for pieces at Safe for X: "+globalWorstSafeX );
		Utils.print("\nWorst Ratio for pieces at Home for O: "+globalWorstHomeO );
		Utils.print("\nWorst Ratio for pieces at Safe for O: "+ globalWorstSafeO);

		Utils.println("\n-------------------------------------------------");
	}
}
