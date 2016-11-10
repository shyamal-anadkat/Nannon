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
 * BayesNetPlayer_ShyamalAnadkat HW3
 * @author SAnadkat
 *
 */
public class BayesNetPlayer_ShyamalAnadkat extends NannonPlayer {


	/*******DECLARE RANDOM VARIABLES**********/
	// Reference : lecture ppt (Jude Shavlik - 2016) 
	private static int boardSize = NannonGameBoard.getCellsOnBoard(); 
	private static int pieces = NannonGameBoard.getPiecesPerPlayer(); 
	private int homeX_win[] = new int[pieces+1];   //holds p(homeX=? | win) 
	private  int homeX_lose[] = new int[pieces+1];  //holds p(homeX=? | !win)
	private int safeX_win[] = new int[pieces+1];   //holds p(safeX=? | win)
	private int safeX_lose[] = new int[pieces+1];  //holds p(safeX=? | !win)
	private int effects_win[] = new int[12];       //holds p(effect=? | win)
	private int effects_loss[] = new int[12];      //holds p(effect=? |!win)
	private int homeO_win[] = new int[pieces+1];   //holds p(homeO=? | win) 
	private int homeO_lose[] = new int[pieces+1];  //holds p(homeO=? | !win)
	private int safeO_win[] = new int[pieces+1];   //holds p(safeO=? | !win)
	private int safeO_lose[] = new int[pieces+1];  //holds p(safeO=? | !win)
	/*********************************************/

	//m-estimates 
	private int winCnt = 1 ; private int lossCnt = 1; 

	//**************best and worst configs/ratios/stats for learned models***************//
	private int best_config[] = new int[6]; private int bad_config[] = new int[6];
	private double globalBestHomeX  = Double.MIN_VALUE, 
			globalBestHomeO  = Double.MIN_VALUE, globalBestSafeX  = Double.MIN_VALUE, 
			globalBestSafeO  = Double.MIN_VALUE, globalBestEffect  = Double.MIN_VALUE, 
			globalBestBH  = Double.MIN_VALUE, globalBestCH  = Double.MIN_VALUE, globalBestOdds = Double.MIN_VALUE;
	private double globalWorstHomeX= Double.MAX_VALUE, globalWorstHomeO= Double.MAX_VALUE, globalWorstSafeX= Double.MAX_VALUE, 
			globalWorstSafeO= Double.MAX_VALUE, globalWorstEffect= Double.MAX_VALUE, globalWorstBH= Double.MAX_VALUE, globalWorstCH = Double.MAX_VALUE;

	//*******EFFECT RANDOM VARIABLES AND NON NAIVE BAYES DEPENDENCIES INIT*********//
	private int extendPrime_win = 1;  private int createPrime_win = 1;
	private int extendPrime_lose = 1; private int createPrime_lose = 1;
	private int hitOpponent_win = 1;  private int brokeMyPrime_win= 1; private int hitOpponent_lose = 1;
	private int brokeMyPrime_lose= 1; private int brokeAndHit_win = 1; private int brokeAndHit_lose = 1; 
	private int createAndHit_win = 1; private int createAndHit_lose = 1; 

	@Override
	public String getPlayerName() { return "Shyamal's Bayes Net Player"; }

	// Constructors.
	public BayesNetPlayer_ShyamalAnadkat() {
		initialize();

	}
	public BayesNetPlayer_ShyamalAnadkat(NannonGameBoard gameBoard) {
		super(gameBoard);
		initialize();
	}

	private void initialize() {
		// Put things here needed for instance creation.
		// m -estimates 
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
	}

	@SuppressWarnings("unused") // This prevents a warning from the "if (false)" below.
	@Override
	public List<Integer> chooseMove(int[] boardConfiguration, List<List<Integer>> legalMoves) {

		double bestProb = Integer.MIN_VALUE;
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
				//ho : hit opp ; bp : break prime, cp : create prime etc...
				double ho_win =1 , ho_lose = 1;
				double bp_win = 1,bp_lose = 1; 
				double ep_win = 1 , ep_lose= 1; 
				double cp_win = 1,cp_lose = 1;
				double bh_win = 1, bh_lose = 1;
				double ch_win = 1, ch_lose = 1; 

				// P(Random Variable given Win) conditional probablities and non-NB dependencies
				double homeXGivenWin = (double) homeX_win[resultingBoard[1]] /(double) winCnt;
				double safeXGivenWin = (double) safeX_win[resultingBoard[3]]/ (double) winCnt;
				double homeOGivenWin = (double) homeO_win[resultingBoard[2]] / (double) winCnt;
				double safeOGivenWin = (double) safeO_win[resultingBoard[4]]/ (double) winCnt;

				//We have hitOpponent and brokeMyprime as dependencies or non NB
				if (hitOpponent) { ho_win = (double) hitOpponent_win / (double) winCnt;}

				if (brokeMyPrime) {bp_win = (double) brokeMyPrime_win / (double) winCnt;}

				if (extendsPrimeOfMine) { ep_win= (double) extendPrime_win / (double) winCnt; }

				if (createsPrimeOfMine) { cp_win = (double) createPrime_win / (double) winCnt; }

				if (hitOpponent & brokeMyPrime) {
					bh_win = (double) brokeAndHit_win / winCnt;
				}
				if (hitOpponent & createsPrimeOfMine) {
					ch_win = (double) createAndHit_win / (double) winCnt;
				}

				// P(random variable given Loss) conditional probs 
				double homeXGivenLoss = (double) homeX_lose[resultingBoard[1]] /(double) lossCnt;
				double safeXGivenLoss = (double) safeX_lose[resultingBoard[3]]/ (double) lossCnt;
				double homeOGivenLoss = (double) homeO_lose[resultingBoard[2]] / (double) lossCnt;
				double safeOGivenLoss = (double) safeO_lose[resultingBoard[4]]/ (double) lossCnt;

				if (hitOpponent) { ho_lose= (double) hitOpponent_lose / (double) lossCnt;}

				if (brokeMyPrime) { bp_lose = (double) brokeMyPrime_lose / (double) lossCnt;}

				if (extendsPrimeOfMine) { ep_lose= (double) extendPrime_lose / (double) lossCnt;}

				if (createsPrimeOfMine) { cp_lose = (double) createPrime_lose / (double) lossCnt; }

				if (hitOpponent & brokeMyPrime) {
					bh_lose = (double) brokeAndHit_lose / (double) lossCnt;
				}
				if (hitOpponent & createsPrimeOfMine) {
					ch_lose = (double) createAndHit_lose / (double) lossCnt;
				}


				double effectGivenWin = (double) effects_win[effect]/(double) winCnt; 
				double effectGivenLoss = (double) effects_loss[effect] /(double) lossCnt;  


				//**********************LEARNED MODEL STATS ********************//
				double homeXRatio = (double) homeXGivenWin / (double) homeXGivenLoss;
				double safeXRatio = (double) safeXGivenWin / (double) safeXGivenLoss;
				double homeORatio = (double) homeOGivenWin / (double) homeOGivenLoss;
				double safeORatio = (double) safeOGivenWin / (double) safeOGivenLoss;
				double effectRatio =  (double)effectGivenWin / (double) effectGivenLoss; 
				double bhRatio = (double) bh_win / (double) bh_lose;
				double chRatio =  (double) ch_win / (double) ch_lose;

				// updating stats for the learned model outputting best ratios for the feautures or each random variable 
				// the feature with highest ratio would be more important one, also this acts as a sanity check for model
				globalBestHomeX = Double.max(globalBestHomeX, homeXRatio);
				globalBestHomeO = Double.max(globalBestHomeO, homeORatio);
				globalBestSafeX = Double.max(globalBestSafeX, safeXRatio);
				globalBestSafeO = Double.max(globalBestSafeO, safeORatio);
				globalBestEffect = Double.max(globalBestEffect, effectRatio);
				globalBestBH = Double.max(globalBestBH, bhRatio); //BREAK AND HIT OPPONENT
				globalBestCH = Double.max(globalBestCH, chRatio); //CREATE AND HIT OPPONENT 

				// updating stats for the learned model outputting worst ratios for the feautures or each random variable 
				globalWorstHomeX = Double.min(globalWorstHomeX, homeXRatio);
				globalWorstHomeO = Double.min(globalWorstHomeO, homeORatio);
				globalWorstSafeX = Double.min(globalWorstSafeX, safeXRatio);
				globalWorstSafeO = Double.min(globalWorstSafeO, safeORatio);
				globalWorstEffect =Double.min(globalWorstEffect, effectRatio);
				globalWorstBH = Double.min(globalBestBH, bhRatio);
				globalWorstCH = Double.min(globalBestCH, chRatio);

				//We have a NB now we need to work on the dependencies between some of the nodes or the random
				//variables. We work on P (RV1 ^ RV2^ RV3 ..... | WIN) 
				//NOTE: I didn't include this in bestODDs again as I compromised for better winning success
				double beyondNBWin = (homeX_win[resultingBoard[1]]* safeX_win[resultingBoard[3]]
						* homeO_win[resultingBoard[2]]*safeO_win[resultingBoard[4]]* effects_win[effect] ) / (double) winCnt; 
				double beyondNBLoss =(homeX_lose[resultingBoard[1]]*
						safeX_lose[resultingBoard[3]] *homeO_lose[resultingBoard[2]]*safeO_lose[resultingBoard[4]]) /(double) lossCnt ;

				//winning and losing probablities 
				double probWin = (double)winCnt / (double) (winCnt + lossCnt);
				double probLoss = (double)lossCnt / (double) (winCnt + lossCnt);

				//assuming independence as well as non-naive dependencies between nodes so we simply multiply them 
				double bestOdds = ( ch_win* ho_win * bp_win* cp_win* ep_win * bh_win* homeXGivenWin * 
						safeXGivenWin * homeOGivenWin* safeOGivenWin * effectGivenWin * probWin)/ 
						(double)     ( ch_lose* bp_lose*  ho_lose * cp_lose* ep_lose * bh_lose* homeXGivenLoss * 
								safeXGivenLoss * homeOGivenLoss* safeOGivenLoss * effectGivenLoss* probLoss);

				if (bestOdds > bestProb) {
					bestProb = bestOdds; 
					chosenMove = move; 
				}
				//updating best odds for learned model 
				if(bestOdds > globalBestOdds) {
					globalBestOdds = bestOdds;
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
		//counter for games lost and won
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
			//     prob(win | currentBoard and chosenMove and chosenMove's Effects)  <--- this is what I (Jude) did, but mainly because at that point I had not yet written getNextBoardConfiguration()
			//     prob(win | resultingBoard and chosenMove's Effects)               <--- condition on the board produced and also on the important changes from the prev board
			//
			//     prob(win | currentBoard and chosenMove)                           <--- if we ignore 'chosenMove's Effects' we would be more in the spirit of a State Board Evaluator (SBE)
			//     prob(win | resultingBoard)                                        <--- but it seems helpful to know something about the impact of the chosen move (ie, in the first two options)
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
			boolean hitOpponent = ManageMoveEffects.isaHit(effect); // Explained in chooseMove() above.
			boolean brokeMyPrime = ManageMoveEffects.breaksPrime( effect);
			boolean extendsPrimeOfMine = ManageMoveEffects.extendsPrime(effect);
			boolean createsPrimeOfMine = ManageMoveEffects.createsPrime(effect);

			if(didIwinThisGame) {
				homeX_win[resultingBoard[1]]++;
				safeX_win[resultingBoard[3]]++;
				homeO_win[resultingBoard[2]]++;
				safeO_win[resultingBoard[4]]++;
				effects_win[effect]++;
				if (hitOpponent) {
					hitOpponent_win++;
				}
				if (brokeMyPrime) {
					brokeMyPrime_win++;
				}
				if (extendsPrimeOfMine) {
					extendPrime_lose++;
				}
				if (createsPrimeOfMine) {
					createPrime_win++;
				}

				if (hitOpponent & brokeMyPrime) {
					brokeAndHit_win++;
				}
				if (hitOpponent & createsPrimeOfMine) {
					createAndHit_win++;
				}

			} else {
				homeX_lose[resultingBoard[1]]++;
				safeX_lose[resultingBoard[3]]++;
				homeO_lose[resultingBoard[2]]++;
				safeO_lose[resultingBoard[4]]++;
				effects_loss[effect]++;
				if (hitOpponent) {
					hitOpponent_lose++;
				}
				if (brokeMyPrime) {
					brokeMyPrime_lose++;
				}
				if (extendsPrimeOfMine) {
					extendPrime_lose++;
				}
				if (createsPrimeOfMine) {
					createPrime_lose++;
				}
				if (hitOpponent & brokeMyPrime) {
					brokeAndHit_lose++;
				}
				if (hitOpponent & createsPrimeOfMine) {
					createAndHit_lose++;
				}
			}
		}
	}

	@Override
	public void reportLearnedModel() { // You can add some code here that reports what was learned, eg the most important feature for WIN and for LOSS.  And/or all the weights on your features.
		Utils.println("\n-------------------------------------------------");
		Utils.println(getPlayerName() + "learning model !!");	
		Utils.println("Best winning odds: "+globalBestOdds);
		Utils.print("\nBest Winning Ratio for effect: "+ globalBestEffect);
		Utils.print("\nBest Winning Ratio for pieces at Home for X: "+globalBestHomeX );
		Utils.print("\nBest Winning Ratio for pieces at Safe for X: "+globalBestSafeX );
		Utils.print("\nBest Winnning Ratio for pieces at Home for O: "+globalBestHomeO );
		Utils.print("\nBest Winning Ratio for pieces at Safe for O: "+ globalBestSafeO);
		Utils.print("\nBest Winning Ratio for Break and Hit Opponent : "+ globalBestBH);
		Utils.print("\nBest Winning Ratio for Create and Hit Opponent dependency : "+ globalBestCH);
		Utils.print("\nWorst Ratio for effect: "+ globalWorstEffect);
		Utils.print("\nWorst Ratio for pieces at Home for X: "+globalWorstHomeX );
		Utils.print("\nWorst Ratio for pieces at Safe for X: "+globalWorstSafeX );
		Utils.print("\nWorst Ratio for pieces at Home for O: "+globalWorstHomeO );
		Utils.print("\nWorst Ratio for pieces at Safe for O: "+ globalWorstSafeO);
		Utils.print("\nWorst Ratio for Break and Hit Opponent : "+ globalWorstBH);
		Utils.print("\nWorst Ratio for Create and Hit Opponent dependency : "+ globalWorstCH);
		Utils.println("\n-------------------------------------------------");
	}
}
