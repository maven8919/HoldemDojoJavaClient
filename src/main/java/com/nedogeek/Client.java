package com.nedogeek;


import org.eclipse.jetty.websocket.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Client {

    private static final List<String> tier1 = Arrays.asList("AA", "KK", "QQ", "JJ", "AKs");
    private static final List<String> tier2 = Arrays.asList("AQs","1010","AK","AJs","KQs","99");
    private static final List<String> tier3 = Arrays.asList("A10s","AQ","KJs","88","K10s","QJs");
    private static final List<String> tier4 = Arrays.asList("A9s","AJ","Q10s","KQ","77","J10s");
    private static final List<String> tier5 = Arrays.asList("A8s","K9s","A10","A5s","A7s");
    private static final List<String> tier6 = Arrays.asList("KJ","66","109s","A4s","Q9s");
    private static final List<String> tier7 = Arrays.asList("J9s","QJ","A6s","55","A3s","K8s","K10");
    private static final List<String> tier8 = Arrays.asList("98s","108s","K7s","A2s");
    private static final List<String> tier9 = Arrays.asList("87s","Q10","Q8s","44","A9","J8s","76s","J10");

    private static final List<String> allTier = Arrays.asList("AA", "KK", "QQ", "JJ", "AKs", "AQs","1010","AK","AJs","KQs","99",
            "A10s","AQ","KJs","88","K10s","QJs", "A9s","AJ","Q10s","KQ","77","J10s", "A8s","K9s","A10","A5s","A7s",
            "KJ","66","109s","A4s","Q9s", "J9s","QJ","A6s","55","A3s","K8s","K10", "98s","108s","K7s","A2s",
            "87s","Q10","Q8s","44","A9","J8s","76s","J10");

    private static final int blind = 10;
    private static final Random random = new Random();

    private static final String userName = "levi";
    private static final String password = "levi";

    private static final String SERVER = "ws://10.0.8.148:8080/ws";
    private org.eclipse.jetty.websocket.WebSocket.Connection connection;

    enum Commands {
        Check, Call, Rise, Fold, AllIn
    }

    class Card implements Comparable<Card> {
        final String suit;
        final String value;

        Card(String suit, String value) {
            this.suit = suit;
            this.value = value;
        }

        public int compareTo(Card o) {
            return this.cardValue() - o.cardValue();
        }

        private int cardValue() {
            int result = -1;
            if ("A".equals(value)) {
                result = 14;
            } else if ("K".equals(value)) {
                result = 13;
            } else if ("Q".equals(value)) {
                result = 12;
            } else if ("J".equals(value)) {
                result = 11;
            } else {
                result = Integer.parseInt(value);
            }
            return result;
        }
    }


    private void con() {
        WebSocketClientFactory factory = new WebSocketClientFactory();
        try {
            factory.start();

        WebSocketClient client = factory.newWebSocketClient();

        connection = client.open(new URI(SERVER + "?user=" + userName + "&password=" + password), new org.eclipse.jetty.websocket.WebSocket.OnTextMessage() {
            public void onOpen(Connection connection) {
                System.out.println("Opened");
            }

            public void onClose(int closeCode, String message) {
                System.out.println("Closed");
            }

            public void onMessage(String data) {
                parseMessage(data);
                System.out.println(data);

                if (userName.equals(mover)) {
                    try {
                        doAnswer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Player {

        final String name;
        final int balance;
        final int bet;
        final String status;
        final List<Card> cards;
        Player(String name, int balance, int bet, String status, List<Card> cards) {
            this.name = name;
            this.balance = balance;
            this.bet = bet;
            this.status = status;
            this.cards = cards;
        }

    }
    List<Card> deskCards;

    int pot;
    String gameRound;

    String dealer;
    String mover;
    List<String> event;
    List<Player> players;

    String cardCombination;

    public Client() {
            con();
    }

    public static void main(String[] args) {
        new Client();
    }

    private void parseMessage(String message) {
        JSONObject json = new JSONObject(message);

        if (json.has("deskPot")) {
            pot = json.getInt("deskPot");
        }
        if (json.has("mover")) {
            mover = json.getString("mover");
        }
        if (json.has("dealer")) {
            dealer = json.getString("dealer");
        }
        if (json.has("gameRound")) {
            gameRound = json.getString("gameRound");
        }
        if (json.has("event")) {
            event = parseEvent(json.getJSONArray("event"));
        }
        if (json.has("players")) {
            players = parsePlayers(json.getJSONArray("players"));
        }

        if (json.has("deskCards")) {
            deskCards = parseCards(((JSONArray) json.get("deskCards")));
        }

        if (json.has("combination")) {
            cardCombination = json.getString("combination");
        }
    }

    private List<String> parseEvent(JSONArray eventJSON) {
        List<String> events = new ArrayList<String>();

        for (int i = 0; i < eventJSON.length(); i++) {
            events.add(eventJSON.getString(i));
        }

        return events;
    }

    private List<Player> parsePlayers(JSONArray playersJSON) {
        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < playersJSON.length(); i++) {
            JSONObject playerJSON = (JSONObject) playersJSON.get(i);
            int balance = 0;
            int bet = 0;
            String status = "";
            String name = "";
            List<Card> cards = new ArrayList<Card>();

            if (playerJSON.has("balance")) {
                balance = playerJSON.getInt("balance");
            }
            if (playerJSON.has("pot")) {
                bet = playerJSON.getInt("pot");
            }
            if (playerJSON.has("status")) {
                status = playerJSON.getString("status");
            }
            if (playerJSON.has("name")) {
                name = playerJSON.getString("name");
            }
            if (playerJSON.has("cards")) {
                cards = parseCards((JSONArray) playerJSON.get("cards"));
            }

            players.add(new Player(name, balance, bet, status, cards));
        }

        return players;
    }

    private List<Card> parseCards(JSONArray cardsJSON) {
        List<Card> cards = new ArrayList<Card>();

        for (int i = 0; i < cardsJSON.length(); i++) {
            String cardSuit = ((JSONObject) cardsJSON.get(i)).getString("cardSuit");
            String cardValue = ((JSONObject) cardsJSON.get(i)).getString("cardValue");

            cards.add(new Card(cardSuit, cardValue));
        }

        return cards;
    }

    private void doAnswer() throws IOException {
        List<Card> myCards = getMyCards(players);
        int amountToCall = Math.max(0, computeAmountToCall(players));
        int myBalance = getMyBalance(players);
        int percentageToCall = (int) ((amountToCall * 100.0f) / myBalance);
        int myPot = getMyPot(players);
        if ("BLIND".equals(gameRound)) {
            if (!handInTopTiers(myCards) && amountToCall > 0) {
                connection.sendMessage(Commands.Fold.toString());
            } else if (!handInTopTiers(myCards) && amountToCall == 0) {
                connection.sendMessage(Commands.Check.toString());
            } else if (handInTopTiers(myCards) && amountToCall == 0) {
                if(Math.random() < 0.5) {
                    connection.sendMessage(Commands.Check.toString());
                } else {
                    int handTier = getHandTier(myCards);
                    int raise = blind * (random.nextInt(handTier - 1) + 1);
                    connection.sendMessage(Commands.Rise.toString() + "," + raise);
                }
            } else {
                int handTier = getHandTier(myCards);
                if (100 - ((handTier - 1) * 10) >= percentageToCall) {
                    connection.sendMessage(Commands.Call.toString());
                } else {
                    connection.sendMessage(Commands.Fold.toString());
                }
            }
        } else if ("THREE_CARDS".equals(gameRound)) {
            if (amountToCall == 0) {
                if (cardCombination.startsWith("High card ")) {
                    connection.sendMessage(Commands.Check.toString());
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Check.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            } else {
                if (cardCombination.startsWith("High card ")) {
                    if (cardCombination.charAt(10) == 'A') {
                        if (percentageToCall <= 10) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    }
                } else if (cardCombination.startsWith("Pair of ")) {
                    if (myPot > 100) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        if (cardCombination.charAt(8) == 'A' || cardCombination.charAt(8) == 'K' || cardCombination.charAt(8) == 'Q' || cardCombination.charAt(8) == 'J') {
                            if (percentageToCall <= 15) {
                                connection.sendMessage(Commands.Call.toString());
                            } else {
                                connection.sendMessage(Commands.Fold.toString());
                            }
                        } else {
                            if (percentageToCall <= 10) {
                                connection.sendMessage(Commands.Call.toString());
                            } else {
                                connection.sendMessage(Commands.Fold.toString());
                            }
                        }
                    }
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            }
        } else if ("FOUR_CARDS".equals(gameRound)) {
            if (amountToCall == 0) {
                if (cardCombination.startsWith("High card ") || cardCombination.startsWith("Pair of ")) {
                    connection.sendMessage(Commands.Check.toString());
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Check.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            } else {
                if (cardCombination.startsWith("High card ")) {
                    if (cardCombination.charAt(10) == 'A') {
                        if (percentageToCall <= 5) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    }
                } else if (cardCombination.startsWith("Pair of ")) {
                    if (cardCombination.charAt(8) == 'A' || cardCombination.charAt(8) == 'K' || cardCombination.charAt(8) == 'Q' || cardCombination.charAt(8) == 'J') {
                        if (percentageToCall <= 10) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    } else {
                        if (percentageToCall <= 8) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    }
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            }
        } else if ("FIVE_CARDS".equals(gameRound)) {
            if (amountToCall == 0) {
                if (cardCombination.startsWith("High card ") || cardCombination.startsWith("Pair of ")) {
                    connection.sendMessage(Commands.Check.toString());
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Check.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            } else {
                if (cardCombination.startsWith("High card ")) {
                    if (cardCombination.charAt(10) == 'A') {
                        if (percentageToCall <= 0) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    }
                } else if (cardCombination.startsWith("Pair of ")) {
                    if (cardCombination.charAt(8) == 'A' || cardCombination.charAt(8) == 'K' || cardCombination.charAt(8) == 'Q' || cardCombination.charAt(8) == 'J') {
                        if (percentageToCall <= 5) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    } else {
                        if (percentageToCall <= 2) {
                            connection.sendMessage(Commands.Call.toString());
                        } else {
                            connection.sendMessage(Commands.Fold.toString());
                        }
                    }
                } else {
                    if(Math.random() < 0.5) {
                        connection.sendMessage(Commands.Call.toString());
                    } else {
                        int handTier = getHandTier(myCards);
                        int raise = blind * (random.nextInt(handTier - 1) + 1);
                        connection.sendMessage(Commands.Rise.toString() + "," + raise);
                    }
                }
            }
        } else {
            connection.sendMessage(Commands.Check.toString());
        }
    }

    private int getMyBalance(List<Player> players) {
        int result = -1;
        for (Player player : players) {
            if (player.name.equals(userName)) {
                result = player.balance;
                break;
            }
        }
        return result;
    }

    private int getMyPot(List<Player> players) {
        int result = -1;
        for (Player player : players) {
            if (player.name.equals(userName)) {
                result = player.bet;
                break;
            }
        }
        return result;
    }

    private int getHandTier(List<Card> myCards) {
        int result = 9;
        String hand = generateTierQualifier(myCards);
        if (tier1.contains(hand)) {
            result = 1;
        } else if (tier2.contains(hand)) {
            result = 2;
        } else if (tier3.contains(hand)) {
            result = 3;
        } else if (tier4.contains(hand)) {
            result = 4;
        } else if (tier5.contains(hand)) {
            result = 5;
        } else if (tier6.contains(hand)) {
            result = 6;
        } else if (tier7.contains(hand)) {
            result = 7;
        } else if (tier8.contains(hand)) {
            result = 8;
        }
        return result;
    }

    private boolean handInTopTiers(List<Card> myCards) {
        String hand = generateTierQualifier(myCards);
        return allTier.contains(hand);
    }

    private String generateTierQualifier(List<Card> myCards) {
        String result = myCards.get(0).value + myCards.get(1).value;
        if (myCards.get(0).suit.equals(myCards.get(1).suit)) {
            result += "s";
        }
        return result;
    }

    private List<Card> getMyCards(List<Player> players) {
        List<Card> result = new ArrayList<Card>();
        for (Player player : players) {
            if (userName.equals(player.name)) {
                result.addAll(player.cards);
            }
        }
        Collections.sort(result);
        return result;
    }

    private int computeAmountToCall(List<Player> players) {
        int myPot = 0;
        int highestAmount = -1;
        for (Player player : players) {
            if (player.name.equals(userName)) {
                myPot = player.bet;
            } else {
                highestAmount = Math.max(highestAmount, player.bet);
            }
        }
        return highestAmount - myPot;
    }
}
