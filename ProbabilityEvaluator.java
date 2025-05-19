import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProbabilityEvaluator {

    private int okeyTileValue;
    private Map<String, EvaluationResult> persStandard;
    private Map<String, EvaluationResult> samePairs;

    public static class TileRepresentation {
        public int value;
        public boolean isJoker;
        public int originalJokerValue;

        public TileRepresentation(int value, boolean isJoker, int originalJokerValue) {
            this.value = value;
            this.isJoker = isJoker;
            this.originalJokerValue = isJoker ? originalJokerValue : -1;
        }

        @Override
        public String toString() {
            String baseStr = Game.getTileRepresentation(value);
            if (isJoker) {
                return Game.getTileRepresentation(originalJokerValue) + " (" + baseStr + " yerine)";
            }
            return baseStr;
        }
    }

    public static class EvaluationResult {
        public int ungroupedTileCount;
        public List<List<TileRepresentation>> bestArrangement;
        public String handType;

        public EvaluationResult(int ungroupedTileCount, List<List<TileRepresentation>> bestArrangement, String handType) {
            this.ungroupedTileCount = ungroupedTileCount;
            this.handType = handType;
            this.bestArrangement = new ArrayList<>();
            if (bestArrangement != null) {
                for (List<TileRepresentation> group : bestArrangement) {
                    this.bestArrangement.add(new ArrayList<>(group));
                }
            }
        }
    }

    public ProbabilityEvaluator(int okeyTileValue) {
        this.okeyTileValue = okeyTileValue;
    }

    public EvaluationResult findBestArrangement(List<Integer> hand) {
        List<Integer> initialOriginalJokerValues = new ArrayList<>();
        Map<Integer, Integer> initialTileCountsMap = new HashMap<>();
        int handSize = hand.size();

        for (int tile : hand) {
            if (tile == okeyTileValue || tile == 52) {
                initialOriginalJokerValues.add(tile);
            } else if (tile >= 0 && tile < 52) {
                initialTileCountsMap.put(tile, initialTileCountsMap.getOrDefault(tile, 0) + 1);
            }
        }

        int[] initialCountsArray = new int[52];
        for (Map.Entry<Integer, Integer> entry : initialTileCountsMap.entrySet()) {
            initialCountsArray[entry.getKey()] = entry.getValue();
        }
        Collections.sort(initialOriginalJokerValues);

        this.persStandard = new HashMap<>();
        EvaluationResult standardResult = solveStandard(initialCountsArray, new ArrayList<>(initialOriginalJokerValues));
        standardResult.handType = "NORMAL";

        this.samePairs = new HashMap<>();
        EvaluationResult pairsResult = solveForPairs(initialCountsArray, new ArrayList<>(initialOriginalJokerValues));
        pairsResult.handType = "ÇİFT";

        boolean perfectDoublesAchieved = (handSize > 0 && handSize % 2 == 0 &&
                pairsResult.ungroupedTileCount == 0 &&
                pairsResult.bestArrangement != null &&
                pairsResult.bestArrangement.size() == handSize / 2);

        if (perfectDoublesAchieved) {
            if (pairsResult.ungroupedTileCount <= standardResult.ungroupedTileCount) {
                return pairsResult;
            }
        }

        if (standardResult.ungroupedTileCount <= pairsResult.ungroupedTileCount) {
            return standardResult;
        } else {
            return pairsResult;
        }
    }

    private String getStateKey(int[] tileCounts, List<Integer> availableJokers) {
        return Arrays.toString(tileCounts) + "_j:" + availableJokers.toString();
    }

    private EvaluationResult solveStandard(int[] currentTileCounts, List<Integer> availableJokers) {
        String stateKey = getStateKey(currentTileCounts, availableJokers);
        if (persStandard.containsKey(stateKey)) {
            EvaluationResult cached = persStandard.get(stateKey);
            return new EvaluationResult(cached.ungroupedTileCount, cached.bestArrangement, "NORMAL");
        }

        int ungroupedIfNoMoreGroups = 0;
        for (int count : currentTileCounts) ungroupedIfNoMoreGroups += count;
        ungroupedIfNoMoreGroups += availableJokers.size();
        EvaluationResult bestOverallResult = new EvaluationResult(ungroupedIfNoMoreGroups, new ArrayList<>(), "NORMAL");


        for (int colorBase = 0; colorBase <= 39; colorBase += 13) {
            for (int startNumInColor = 0; startNumInColor <= 12; startNumInColor++) {
                for (int runLength = 3; runLength <= 13; runLength++) {
                    if (startNumInColor + runLength > 13) break;

                    int[] nextCounts = Arrays.copyOf(currentTileCounts, currentTileCounts.length);
                    List<Integer> jokersCopy = new ArrayList<>(availableJokers);
                    List<TileRepresentation> groupRep = new ArrayList<>();

                    for (int i = 0; i < runLength; i++) {
                        int target = colorBase + startNumInColor + i;
                        if (target < 0 || target >= 52) {
                            break; }
                        if (nextCounts[target] > 0) {
                            nextCounts[target]--;
                            groupRep.add(new TileRepresentation(target, false, -1));
                        } else {
                            boolean jokerUsed = tryUseJokerForTarget(target, jokersCopy, groupRep);
                            if (!jokerUsed) {
                                break; }
                        }
                    }

                }
            }
        }

        for (int numValue = 0; numValue <= 12; numValue++) {
            int[] targets = {numValue, numValue + 13, numValue + 26, numValue + 39};
            for (int setSize = 3; setSize <= 4; setSize++) {
                List<int[]> combos = getCombinationsIndices(4, setSize);
                for (int[] comboIdxArray : combos) {
                    int[] nextCounts = Arrays.copyOf(currentTileCounts, currentTileCounts.length);
                    List<Integer> jokersCopy = new ArrayList<>(availableJokers);
                    List<TileRepresentation> groupRep = new ArrayList<>();
                    boolean possible = true;
                    for (int colorIndexValue : comboIdxArray) {
                        int target = targets[colorIndexValue];
                        if (target < 0 || target >= 52) { possible = false; break; }
                        if (nextCounts[target] > 0) {
                            nextCounts[target]--;
                            groupRep.add(new TileRepresentation(target, false, -1));
                        } else {
                            boolean jokerUsed = tryUseJokerForTarget(target, jokersCopy, groupRep);
                            if (!jokerUsed) { possible = false; break; }
                        }
                    }
                    if (!possible) continue;

                    if (possible) {
                        EvaluationResult subRes = solveStandard(nextCounts, jokersCopy);
                        if (subRes.ungroupedTileCount < bestOverallResult.ungroupedTileCount) {
                            List<List<TileRepresentation>> newArr = new ArrayList<>(subRes.bestArrangement);
                            newArr.add(groupRep);
                            bestOverallResult = new EvaluationResult(subRes.ungroupedTileCount, newArr, "NORMAL");
                        }
                    }
                }
            }
        }

        EvaluationResult finalResult = new EvaluationResult(bestOverallResult.ungroupedTileCount, bestOverallResult.bestArrangement, "NORMAL");
        persStandard.put(stateKey, finalResult);
        return finalResult;
    }

    private EvaluationResult solveForPairs(int[] currentTileCounts, List<Integer> availableJokers) {
        String stateKey = getStateKey(currentTileCounts, availableJokers);
        if (samePairs.containsKey(stateKey)) {
            EvaluationResult cached = samePairs.get(stateKey);
            return new EvaluationResult(cached.ungroupedTileCount, cached.bestArrangement, "ÇİFT");
        }

        int ungroupedIfNoMorePairs = 0;
        for (int count : currentTileCounts) ungroupedIfNoMorePairs += count;
        ungroupedIfNoMorePairs += availableJokers.size();
        EvaluationResult bestOverallResult = new EvaluationResult(ungroupedIfNoMorePairs, new ArrayList<>(), "ÇİFT");

        for (int tileValueToPair = 0; tileValueToPair < 52; tileValueToPair++) {
            if (currentTileCounts[tileValueToPair] >= 2) {
                int[] nextCounts = Arrays.copyOf(currentTileCounts, currentTileCounts.length);
                nextCounts[tileValueToPair] -= 2;

                List<TileRepresentation> pairRep = new ArrayList<>();
                pairRep.add(new TileRepresentation(tileValueToPair, false, -1));
                pairRep.add(new TileRepresentation(tileValueToPair, false, -1));

                EvaluationResult subRes = solveForPairs(nextCounts, new ArrayList<>(availableJokers));
                if (subRes.ungroupedTileCount < bestOverallResult.ungroupedTileCount) {
                    List<List<TileRepresentation>> newArr = new ArrayList<>(subRes.bestArrangement);
                    newArr.add(pairRep);
                    bestOverallResult = new EvaluationResult(subRes.ungroupedTileCount, newArr, "ÇİFT");
                }
            }

            if (currentTileCounts[tileValueToPair] >= 1 && !availableJokers.isEmpty()) {
                int[] nextCounts = Arrays.copyOf(currentTileCounts, currentTileCounts.length);
                nextCounts[tileValueToPair]--;

                List<Integer> jokersCopyForThisAttempt = new ArrayList<>(availableJokers);
                List<TileRepresentation> pairRep = new ArrayList<>();
                pairRep.add(new TileRepresentation(tileValueToPair, false, -1));

                boolean jokerUsedForPair = false;
                int actualOkeyJokerIndex = -1;
                for(int jokerIdx = 0; jokerIdx < jokersCopyForThisAttempt.size(); jokerIdx++){
                    if(jokersCopyForThisAttempt.get(jokerIdx) == okeyTileValue){
                        actualOkeyJokerIndex = jokerIdx;
                        break;
                    }
                }
                if(actualOkeyJokerIndex != -1){
                    int jokerOriginalVal = jokersCopyForThisAttempt.remove(actualOkeyJokerIndex);
                    pairRep.add(new TileRepresentation(tileValueToPair, true, jokerOriginalVal));
                    jokerUsedForPair = true;
                } else {
                    int fakeOkeyJokerIndex = -1;
                    for(int jokerIdx = 0; jokerIdx < jokersCopyForThisAttempt.size(); jokerIdx++){
                        if(jokersCopyForThisAttempt.get(jokerIdx) == 52){
                            fakeOkeyJokerIndex = jokerIdx;
                            break;
                        }
                    }
                    if (fakeOkeyJokerIndex != -1 && tileValueToPair == okeyTileValue) {
                        int jokerOriginalVal = jokersCopyForThisAttempt.remove(fakeOkeyJokerIndex);
                        pairRep.add(new TileRepresentation(tileValueToPair, true, jokerOriginalVal));
                        jokerUsedForPair = true;
                    }
                }

                if (jokerUsedForPair) {
                    EvaluationResult subRes = solveForPairs(nextCounts, jokersCopyForThisAttempt);
                    if (subRes.ungroupedTileCount < bestOverallResult.ungroupedTileCount) {
                        List<List<TileRepresentation>> newArr = new ArrayList<>(subRes.bestArrangement);
                        newArr.add(pairRep);
                        bestOverallResult = new EvaluationResult(subRes.ungroupedTileCount, newArr, "ÇİFT");
                    }
                }
            }
        }

        EvaluationResult finalResult = new EvaluationResult(bestOverallResult.ungroupedTileCount, bestOverallResult.bestArrangement, "ÇİFT");
        samePairs.put(stateKey, finalResult);
        return finalResult;
    }

    private boolean tryUseJokerForTarget(int targetTile, List<Integer> availableJokers, List<TileRepresentation> groupRep) {
        int actualOkeyJokerIndex = -1;
        for(int jokerIdx = 0; jokerIdx < availableJokers.size(); jokerIdx++){
            if(availableJokers.get(jokerIdx) == okeyTileValue){
                actualOkeyJokerIndex = jokerIdx;
                break;
            }
        }
        if(actualOkeyJokerIndex != -1){
            int jokerOriginalVal = availableJokers.remove(actualOkeyJokerIndex);
            groupRep.add(new TileRepresentation(targetTile, true, jokerOriginalVal));
            return true;
        }

        int fakeOkeyJokerIndex = -1;
        for(int jokerIdx = 0; jokerIdx < availableJokers.size(); jokerIdx++){
            if(availableJokers.get(jokerIdx) == 52){
                fakeOkeyJokerIndex = jokerIdx;
                break;
            }
        }
        if (fakeOkeyJokerIndex != -1 && targetTile == okeyTileValue) {
            int jokerOriginalVal = availableJokers.remove(fakeOkeyJokerIndex);
            groupRep.add(new TileRepresentation(targetTile, true, jokerOriginalVal));
            return true;
        }
        return false;
    }

    private List<int[]> getCombinationsIndices(int n, int k) {
        List<int[]> allCombinations = new ArrayList<>();
        if (k < 0 || k > n) {
            return allCombinations;
        }
        int[] currentCombination = new int[k];
        if (k == 0) {
            allCombinations.add(currentCombination);
            return allCombinations;
        }

        for (int i = 0; i < k; i++) {
            currentCombination[i] = i;
        }
        allCombinations.add(Arrays.copyOf(currentCombination, k));
        while (true) {
            int i = k - 1;
            while (i >= 0 && currentCombination[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            currentCombination[i]++;
            for (int j = i + 1; j < k; j++) {
                currentCombination[j] = currentCombination[j - 1] + 1;
            }
            allCombinations.add(Arrays.copyOf(currentCombination, k));
        }

        return allCombinations;
    }

}