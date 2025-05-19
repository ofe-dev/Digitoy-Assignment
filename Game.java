import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Game {


    public static void main(String[] args) {
        List<Integer> deckTiles = new ArrayList<>();
        for (int i = 0; i <= 52; i++) {
            deckTiles.add(i);
            deckTiles.add(i);
        }
        Collections.shuffle(deckTiles);
        System.out.println("Taşlar karıştırıldı.");
        int indicatorTile = -1;
        int indexOfIndicatorInDeck = -1;
        for (int i = 0; i < deckTiles.size(); i++) {
            if (deckTiles.get(i) != 52) {
                indicatorTile = deckTiles.get(i);
                indexOfIndicatorInDeck = i;
                break;
            }
        }
        deckTiles.remove(indexOfIndicatorInDeck);
        System.out.println("Gösterge Taşı: " + getTileRepresentation(indicatorTile) + " [" + indicatorTile + "]");
        int okeyTileValue = determineOkeyTileValue(indicatorTile);
        System.out.println("Okey Taşı: " + getTileRepresentation(okeyTileValue) + " [" + okeyTileValue + "]");

        List<List<Integer>> playerHands = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            playerHands.add(new ArrayList<>());
        }
        for (int i = 0; i < 15; i++) {
            playerHands.get(0).add(deckTiles.remove(0));
        }
        for (int pIdx = 1; pIdx < 4; pIdx++) {
            for (int i = 0; i < 14; i++) {
                playerHands.get(pIdx).add(deckTiles.remove(0));
            }
        }

        for (int i = 0; i < playerHands.size(); i++) {
            List<Integer> hand = playerHands.get(i);
            Collections.sort(hand);
            System.out.print("\nOyuncu " + (i + 1) + " Eli (" + hand.size() + " taş): ");
            StringBuilder handDisplay = new StringBuilder();
            for (int j = 0; j < hand.size(); j++) {
                handDisplay.append(getTileRepresentation(hand.get(j)))
                        .append(" [").append(hand.get(j)).append("]");
                if (j < hand.size() - 1) handDisplay.append(", ");
            }
            System.out.println(handDisplay.toString());
        }

        System.out.println("\n--- Kazanma Potansiyeli Değerlendiriliyor ---");
        ProbabilityEvaluator evaluator = new ProbabilityEvaluator(okeyTileValue);

        List<ProbabilityEvaluator.EvaluationResult> allPlayerResults = new ArrayList<>();
        int minUngroupedOverall = Integer.MAX_VALUE;

        for (int i = 0; i < playerHands.size(); i++) {
            List<Integer> hand = playerHands.get(i);
            System.out.println("\nOyuncu " + (i + 1) + " eli değerlendiriliyor ("+ hand.size() +" taş)...");
            System.out.println("  (Bu el için Okey Taşı: " + getTileRepresentation(okeyTileValue) + " [" + okeyTileValue + "])");


            ProbabilityEvaluator.EvaluationResult result = evaluator.findBestArrangement(new ArrayList<>(hand));
            allPlayerResults.add(result);

            int reportedUngroupedCount = result.ungroupedTileCount;
            int groupedTileCountInArrangement = 0;

            if (result.bestArrangement != null) {
                for (List<ProbabilityEvaluator.TileRepresentation> group : result.bestArrangement) {
                    groupedTileCountInArrangement += group.size();
                }
            }

            List<Integer> actualUngroupedTiles = new ArrayList<>(hand);
            if (result.bestArrangement != null) {
                for (List<ProbabilityEvaluator.TileRepresentation> group : result.bestArrangement) {
                    for (ProbabilityEvaluator.TileRepresentation tileRep : group) {
                        if (tileRep.isJoker) {
                            actualUngroupedTiles.remove(Integer.valueOf(tileRep.originalJokerValue));
                        } else {
                            actualUngroupedTiles.remove(Integer.valueOf(tileRep.value));
                        }
                    }
                }
            }
            Collections.sort(actualUngroupedTiles);

            System.out.println("  Değerlendirme Tipi: " + result.handType);
            System.out.println("  Gruplanmış taş sayısı: " + groupedTileCountInArrangement);
            System.out.println("  Gruplanmamış taş sayısı: " + reportedUngroupedCount);

            System.out.print("  Gruplanmamış taşlar: [");
            for (int k = 0; k < actualUngroupedTiles.size(); k++) {
                System.out.print(getTileRepresentation(actualUngroupedTiles.get(k)) + " [" + actualUngroupedTiles.get(k) + "]");
                if (k < actualUngroupedTiles.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");

            System.out.println("  En İyi Grup Düzenlemesi:");
            if (result.bestArrangement == null || result.bestArrangement.isEmpty()) {
                System.out.println("    (Hiçbir grup oluşturulamadı)");
            } else {
                for (List<ProbabilityEvaluator.TileRepresentation> group : result.bestArrangement) {
                    System.out.print("    Grup: [");
                    for (int k = 0; k < group.size(); k++) {
                        System.out.print(group.get(k).toString());
                        if (k < group.size() - 1) System.out.print(", ");
                    }
                    System.out.println("]");
                }
            }

            if (result.ungroupedTileCount < minUngroupedOverall) {
                minUngroupedOverall = result.ungroupedTileCount;
            }
        }

        List<Integer> tiedPlayerIndices = new ArrayList<>();
        for(int i=0; i<allPlayerResults.size(); i++){
            if(allPlayerResults.get(i) != null && allPlayerResults.get(i).ungroupedTileCount == minUngroupedOverall){
                tiedPlayerIndices.add(i);
            }
        }

        int finalBestPlayerIndex = -1;

        if (tiedPlayerIndices.isEmpty()){
            System.out.println("\n>> En yüksek kazanma potansiyeline sahip oyuncu belirlenemedi.");
        } else if (tiedPlayerIndices.size() == 1) {
            finalBestPlayerIndex = tiedPlayerIndices.get(0);
            ProbabilityEvaluator.EvaluationResult bestResult = allPlayerResults.get(finalBestPlayerIndex);
            System.out.println("\n>> En Yüksek Kazanma Potansiyeline Sahip Oyuncu: " + (finalBestPlayerIndex + 1) +
                    " (" + minUngroupedOverall + " adet gruplanmamış taş. Gruplandırma Tipi: " + bestResult.handType + ")");
        } else {
            System.out.println("\n--- Beraberlik Durumu Tespit Edildi (" + minUngroupedOverall + " gruplanmamış taş ile " + tiedPlayerIndices.size() + " oyuncu arasında) ---");
            double maxTieBreakerScore = -Double.MAX_VALUE;
            finalBestPlayerIndex = tiedPlayerIndices.get(0);

            for (int tiedIndex : tiedPlayerIndices) {
                List<Integer> originalHand = playerHands.get(tiedIndex);
                ProbabilityEvaluator.EvaluationResult tiedPlayerResult = allPlayerResults.get(tiedIndex);

                List<Integer> actualUngroupedTiles = new ArrayList<>(originalHand);
                if (tiedPlayerResult.bestArrangement != null) {
                    for (List<ProbabilityEvaluator.TileRepresentation> group : tiedPlayerResult.bestArrangement) {
                        for (ProbabilityEvaluator.TileRepresentation tileRep : group) {
                            if (tileRep.isJoker) {
                                actualUngroupedTiles.remove(Integer.valueOf(tileRep.originalJokerValue));
                            } else {
                                actualUngroupedTiles.remove(Integer.valueOf(tileRep.value));
                            }
                        }
                    }
                }
                Collections.sort(actualUngroupedTiles);

                double tieBreakerScore = calculateTieBreakerScore(actualUngroupedTiles, okeyTileValue, tiedPlayerResult.handType, new ArrayList<>(deckTiles));

                if (tieBreakerScore > maxTieBreakerScore) {
                    maxTieBreakerScore = tieBreakerScore;
                    finalBestPlayerIndex = tiedIndex;
                } else if (tieBreakerScore == maxTieBreakerScore) {
                    if (tiedIndex < finalBestPlayerIndex) {
                        finalBestPlayerIndex = tiedIndex;
                    }
                }
            }
            ProbabilityEvaluator.EvaluationResult finalBestEval = allPlayerResults.get(finalBestPlayerIndex);
            System.out.println("\n>> Beraberlik Sonrası En Yüksek Potansiyel: Oyuncu " + (finalBestPlayerIndex + 1));
        }

        System.out.println("\nDestede Kalan Taş Sayısı: " + deckTiles.size());
        Collections.sort(deckTiles);
        System.out.print("Destede Kalan Taşlar: ");
        StringBuilder remainingDeckDisplay = new StringBuilder();
        for (int i = 0; i < deckTiles.size(); i++) {
            remainingDeckDisplay.append(getTileRepresentation(deckTiles.get(i)))
                    .append(" [").append(deckTiles.get(i)).append("]");
            if (i < deckTiles.size() - 1) remainingDeckDisplay.append(", ");
        }
        System.out.println(remainingDeckDisplay.toString());
    }

    public static String getTileRepresentation(int tileValue) {
        if (tileValue == 52) {
            return "Sahte Okey";
        }
        String color = "";
        int number = 0;
        if (tileValue >= 0 && tileValue <= 12) {
            color = "Sarı";
            number = tileValue + 1;
        } else if (tileValue >= 13 && tileValue <= 25) {
            color = "Mavi";
            number = (tileValue - 13) + 1;
        } else if (tileValue >= 26 && tileValue <= 38) {
            color = "Siyah";
            number = (tileValue - 26) + 1;
        } else if (tileValue >= 39 && tileValue <= 51) {
            color = "Kırmızı";
            number = (tileValue - 39) + 1;
        }
        return color + " " + number;
    }

    public static int determineOkeyTileValue(int indicatorTileValue) {
        int colorGroupBase = 0;
        int numberInColor = 0;
        if (indicatorTileValue >= 0 && indicatorTileValue <= 12) {
            colorGroupBase = 0;
        } else if (indicatorTileValue >= 13 && indicatorTileValue <= 25) {
            colorGroupBase = 13;
        } else if (indicatorTileValue >= 26 && indicatorTileValue <= 38) {
            colorGroupBase = 26;
        } else {
            colorGroupBase = 39;
        }
        numberInColor = indicatorTileValue - colorGroupBase;
        int okeyNumberInColor;
        if (numberInColor == 12) {
            okeyNumberInColor = 0;
        } else {
            okeyNumberInColor = numberInColor + 1;
        }
        return colorGroupBase + okeyNumberInColor;
    }

    public static double calculateTieBreakerScore(List<Integer> ungroupedTiles,
                                                  int okeyTileValue,
                                                  String handType,
                                                  List<Integer> remainingDeckTiles) {
        double score = 0;

        Map<Integer, Integer> deckTileCounts = new HashMap<>();
        for (int tile : remainingDeckTiles) {
            deckTileCounts.put(tile, deckTileCounts.getOrDefault(tile, 0) + 1);
        }

        Map<Integer, Integer> ungroupedTileCountsMap = new HashMap<>();
        int actualOkeyTileInUngroupedCount = 0;
        List<Integer> sortedNonJokerUngrouped = new ArrayList<>();

        for (int tile : ungroupedTiles) {
            if (tile == okeyTileValue) {
                actualOkeyTileInUngroupedCount++;
            } else if (tile >= 0 && tile < 52) {
                ungroupedTileCountsMap.put(tile, ungroupedTileCountsMap.getOrDefault(tile, 0) + 1);
                sortedNonJokerUngrouped.add(tile);
            }
        }
        Collections.sort(sortedNonJokerUngrouped);

        score += actualOkeyTileInUngroupedCount * 10.0;

        double pairPotentialScore = 0;
        double runPotentialScore = 0;


        List<Integer> uniqueUngroupedNonJokers = new ArrayList<>(ungroupedTileCountsMap.keySet());
        for (int tileValue : uniqueUngroupedNonJokers) {
            int countInHand = ungroupedTileCountsMap.get(tileValue);
            if (countInHand >= 2) {
                pairPotentialScore += 3.0 * (countInHand / 2);
            }
            if (deckTileCounts.getOrDefault(tileValue, 0) > 0) {
                pairPotentialScore += 2.0;
            }
            int tileNumberValue = tileValue % 13;
            int neededForSet3 = 0;
            for(int colorOffset = 0; colorOffset <=39; colorOffset += 13){
                int potentialSetMate = tileNumberValue + colorOffset;
                if(potentialSetMate == tileValue || ungroupedTileCountsMap.containsKey(potentialSetMate)) continue;
                if(deckTileCounts.getOrDefault(potentialSetMate, 0) > 0) neededForSet3++;
            }
            if(countInHand == 1 && neededForSet3 >= 1){
                pairPotentialScore += 1.0 * Math.min(neededForSet3, 2);
            }
        }

        Map<String, Boolean> countedRunPairs = new HashMap<>();
        for(int i=0; i < sortedNonJokerUngrouped.size() -1; i++){
            int t1 = sortedNonJokerUngrouped.get(i);
            int t2 = sortedNonJokerUngrouped.get(i+1);
            int colorBase1 = (t1/13) * 13;
            int colorBase2 = (t2/13) * 13;
            if(colorBase1 == colorBase2 && t1 + 1 == t2){
                String runPairKey = colorBase1 + "_" + t1 + "_" + t2;
                if(!countedRunPairs.containsKey(runPairKey)){
                    runPotentialScore += 2.0;
                    countedRunPairs.put(runPairKey, true);
                }
            }
        }

        for (int tileValue : uniqueUngroupedNonJokers) {
            int tileColorBase = (tileValue / 13) * 13;
            int tileNumInColor = tileValue % 13;

            if (tileNumInColor > 0 && deckTileCounts.getOrDefault(tileValue - 1, 0) > 0) {
                runPotentialScore += 1.5;
            }
            if (tileNumInColor < 12 && deckTileCounts.getOrDefault(tileValue + 1, 0) > 0) {
                runPotentialScore += 1.5;
            }
            if (tileNumInColor < 11 && ungroupedTileCountsMap.containsKey(tileValue + 2) &&
                    (((tileValue+2)/13)*13) == tileColorBase &&
                    deckTileCounts.getOrDefault(tileValue + 1, 0) > 0) {
                runPotentialScore += 3.5;
            }
        }

        if ("ÇİFT".equals(handType)) {
            score += pairPotentialScore * 1.5;
            score += runPotentialScore * 0.2;
        } else {
            score += pairPotentialScore;
            score += runPotentialScore;
        }

        return score;
    }

}