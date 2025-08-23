import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BlackjackCasino extends JFrame {
    static Random random = new Random();
    java.util.List<Card> player, dealer; // hands
    Deck deck;
    double balance = 500.0;
    double bet = 0.0;

    JTextPane gameLog;
    JButton hitBtn, standBtn, dealBtn, doubleBtn, splitBtn, playAgainBtn, exitBtn;
    JTextField betField;
    JLabel balanceLabel;
    JCheckBox coachModeCheck;

    // Coaching Window
    JFrame coachingFrame;
    JTextArea coachingText;

    // --- Card class ---
    static class Card {
        String rank, suit;
        int value;
        Card(String rank, String suit, int value) {
            this.rank = rank;
            this.suit = suit;
            this.value = value;
        }
        public String toString() {
            return rank + " of " + suit;
        }
    }

    // --- Deck / Shoe class ---
    static class Deck {
        java.util.List<Card> cards = new ArrayList<>();
        int cutCardPosition;

        Deck() {
            String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
            String[] ranks = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
            int[] values = {2,3,4,5,6,7,8,9,10,10,10,10,11};

            // 6-deck shoe
            for (int d = 0; d < 6; d++) {
                for (String suit : suits) {
                    for (int i = 0; i < ranks.length; i++) {
                        cards.add(new Card(ranks[i], suit, values[i]));
                    }
                }
            }
            Collections.shuffle(cards);

            // Cut card ~75–80% penetration
            cutCardPosition = cards.size() - (52 + random.nextInt(26));
        }

        Card draw() { return cards.remove(0); }
        boolean cutCardReached() { return cards.size() <= cutCardPosition; }
    }

    // --- Hand Value ---
    static int handValue(java.util.List<Card> hand) {
        int value = 0, aces = 0;
        for (Card c : hand) {
            value += c.value;
            if (c.rank.equals("A")) aces++;
        }
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }
        return value;
    }

    // --- Constructor ---
    public BlackjackCasino() {
        setTitle("Las Vegas Blackjack");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gameLog = new JTextPane();
        gameLog.setEditable(false);
        gameLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(gameLog);

        JPanel controlPanel = new JPanel();
        balanceLabel = new JLabel("Balance: $" + balance);
        betField = new JTextField(5);
        dealBtn = new JButton("Deal");
        hitBtn = new JButton("Hit");
        standBtn = new JButton("Stand");
        doubleBtn = new JButton("Double Down");
        splitBtn = new JButton("Split");
        coachModeCheck = new JCheckBox("Coach Mode");
        playAgainBtn = new JButton("Play Again");
        exitBtn = new JButton("Exit");

        hitBtn.setEnabled(false);
        standBtn.setEnabled(false);
        doubleBtn.setEnabled(false);
        splitBtn.setEnabled(false);
        playAgainBtn.setEnabled(false);

        controlPanel.add(balanceLabel);
        controlPanel.add(new JLabel("Bet: $"));
        controlPanel.add(betField);
        controlPanel.add(dealBtn);
        controlPanel.add(hitBtn);
        controlPanel.add(standBtn);
        controlPanel.add(doubleBtn);
        controlPanel.add(splitBtn);
        controlPanel.add(coachModeCheck);
        controlPanel.add(playAgainBtn);
        controlPanel.add(exitBtn);

        add(scroll, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        appendLog("Welcome to Las Vegas Blackjack!\nPlace your bet and press Deal.\n", Color.BLACK);

        // Buttons
        dealBtn.addActionListener(e -> startRound());
        hitBtn.addActionListener(e -> hit());
        standBtn.addActionListener(e -> stand());
        doubleBtn.addActionListener(e -> doubleDown());
        splitBtn.addActionListener(e -> split());
        playAgainBtn.addActionListener(e -> restartGame());
        exitBtn.addActionListener(e -> System.exit(0));

        // Setup coaching window
        setupCoachingWindow();
    }

    // --- Coaching Window setup ---
    void setupCoachingWindow() {
        coachingFrame = new JFrame("Coaching");
        coachingFrame.setSize(300, 200);
        coachingText = new JTextArea();
        coachingText.setEditable(false);
        coachingText.setWrapStyleWord(true);
        coachingText.setLineWrap(true);
        coachingFrame.add(new JScrollPane(coachingText));
        coachingFrame.setVisible(false);
    }

    void showCoachingAdvice(String advice, String explanation) {
        coachingText.setText("Recommendation: " + advice + "\n\n" + explanation);
        coachingFrame.setVisible(true);
    }

    // --- Start Round ---
    void startRound() {
        clearLog();
        try { bet = Double.parseDouble(betField.getText()); }
        catch (Exception e) { appendLog("Enter a valid bet.\n", Color.BLACK); return; }
        if (bet <= 0 || bet > balance) { appendLog("Invalid bet amount.\n", Color.BLACK); return; }

        if (deck == null || deck.cutCardReached()) {
            deck = new Deck();
            appendLog("New 6-deck shoe shuffled and ready!\n", Color.BLACK);
        }

        player = new ArrayList<>();
        dealer = new ArrayList<>();
        player.add(deck.draw());
        player.add(deck.draw());
        dealer.add(deck.draw());
        dealer.add(deck.draw());

        appendLog("New Round! You bet $" + bet + "\n", Color.BLACK);

        dealBtn.setEnabled(false);
        hitBtn.setEnabled(true);
        standBtn.setEnabled(true);
        doubleBtn.setEnabled(balance >= bet * 2 && player.size() == 2);
        splitBtn.setEnabled(balance >= bet && player.size() == 2 &&
                            player.get(0).rank.equals(player.get(1).rank));

        printHands(true);

        if (coachModeCheck.isSelected() && player.size() >= 2) {
            String advice = getAdvancedAdvice(player, dealer.get(1));
            String explanation = getAdviceExplanation(player, dealer.get(1), advice);
            showCoachingAdvice(advice, explanation);
        }
    }

    // --- Player Hits ---
    void hit() {
        player.add(deck.draw());
        printHands(true);
        doubleBtn.setEnabled(balance >= bet * 2 && player.size() == 2);
        splitBtn.setEnabled(balance >= bet && player.size() == 2 &&
                            player.get(0).rank.equals(player.get(1).rank));
        if (coachModeCheck.isSelected() && handValue(player) <= 21) {
            String advice = getAdvancedAdvice(player, dealer.get(1));
            String explanation = getAdviceExplanation(player, dealer.get(1), advice);
            showCoachingAdvice(advice, explanation);
        }
        if (handValue(player) > 21) {
            appendLog("You busted! Lose $" + bet + "\n", Color.BLACK);
            balance -= bet;
            endRound();
        }
    }

    // --- Player Stands ---
    void stand() {
        appendLog("You stand. Dealer reveals:\n", Color.BLACK);
        printHands(false);
        while (handValue(dealer) < 17) {
            Card c = deck.draw();
            dealer.add(c);
            appendLog("Dealer draws: " + c + "\n", Color.RED);
        }
        int dealerValue = handValue(dealer);
        int playerValue = handValue(player);
        appendLog("Final Dealer Hand: (Total: " + dealerValue + ")\n", Color.RED);

        if (dealerValue > 21 || playerValue > dealerValue) {
            appendLog("You win! Gain $" + bet + "\n", Color.BLACK);
            balance += bet;
        } else if (playerValue == dealerValue) {
            appendLog("Push! Bet returned.\n", Color.BLACK);
        } else {
            appendLog("Dealer wins! Lose $" + bet + "\n", Color.BLACK);
            balance -= bet;
        }
        endRound();
    }

    // --- Double Down ---
    void doubleDown() {
        if (balance < bet * 2) {
            appendLog("Not enough balance to double down.\n", Color.BLACK);
            return;
        }
        balance -= bet; // subtract extra bet only
        bet *= 2;
        appendLog("Double down! New bet: $" + bet + "\n", Color.BLACK);
        hit();
        if (handValue(player) <= 21) stand();
    }

    void split() {
        appendLog("Split not implemented in this simplified version.\n", Color.BLACK);
    }

    // --- End Round ---
    void endRound() {
        balanceLabel.setText("Balance: $" + balance);
        hitBtn.setEnabled(false);
        standBtn.setEnabled(false);
        doubleBtn.setEnabled(false);
        splitBtn.setEnabled(false);
        dealBtn.setEnabled(true);
        if (balance <= 0) {
            appendLog("Game over! You're out of money.\n", Color.BLACK);
            dealBtn.setEnabled(false);
            playAgainBtn.setEnabled(true);
        }
    }

    // --- Restart Game ---
    void restartGame() {
        balance = 500.0;
        balanceLabel.setText("Balance: $" + balance);
        playAgainBtn.setEnabled(false);
        dealBtn.setEnabled(true);
        clearLog();
        appendLog("Welcome to Las Vegas Blackjack!\nPlace your bet and press Deal.\n", Color.BLACK);
    }

    // --- Coaching logic ---
    String getAdvancedAdvice(java.util.List<Card> playerHand, Card dealerUpCard) {
        int playerTotal = handValue(playerHand);
        int dealerValue = dealerUpCard.value;

        if (playerHand.size() == 2 && playerHand.get(0).rank.equals(playerHand.get(1).rank)) {
            String rank = playerHand.get(0).rank;
            switch(rank) {
                case "A": case "8": return "Split";
                case "10": case "J": case "Q": case "K": return "Stand";
                case "9": return (dealerValue >= 2 && dealerValue <= 6 || dealerValue == 8 || dealerValue == 9) ? "Split" : "Stand";
                case "7": return (dealerValue >= 2 && dealerValue <= 7) ? "Split" : "Hit";
                case "6": return (dealerValue >= 2 && dealerValue <= 6) ? "Split" : "Hit";
                case "5": return (dealerValue >= 2 && dealerValue <= 9) ? "Double Down" : "Hit";
                case "4": return (dealerValue == 5 || dealerValue == 6) ? "Split" : "Hit";
                case "3": case "2": return (dealerValue >= 2 && dealerValue <= 7) ? "Split" : "Hit";
            }
        }
        if (playerTotal >= 17) return "Stand";
        if (playerTotal >= 13 && playerTotal <= 16) return (dealerValue >= 2 && dealerValue <= 6) ? "Stand" : "Hit";
        if (playerTotal == 12) return (dealerValue >= 4 && dealerValue <= 6) ? "Stand" : "Hit";
        if (playerTotal == 11) return "Double Down";
        if (playerTotal == 10) return (dealerValue <= 9) ? "Double Down" : "Hit";
        if (playerTotal == 9) return (dealerValue >= 3 && dealerValue <= 6) ? "Double Down" : "Hit";
        return "Hit";
    }

    String getAdviceExplanation(java.util.List<Card> playerHand, Card dealerUpCard, String advice) {
        int total = handValue(playerHand);
        return "Basic strategy recommends " + advice +
               " on " + total + " against dealer's " + dealerUpCard;
    }

    // --- Print Hands ---
    void printHands(boolean hideDealerCard) {
        StringBuilder sb = new StringBuilder();
        sb.append("======= Current Hands =======\n");
        sb.append(String.format("%-20s| Player\n", "Dealer"));

        int maxCards = Math.max(dealer.size(), player.size());
        for (int i = 0; i < maxCards; i++) {
            String dealerCard = "";
            if (i < dealer.size()) {
                if (i == 0 && hideDealerCard) dealerCard = "?? of ??";
                else dealerCard = dealer.get(i).toString();
            }
            String playerCard = i < player.size() ? player.get(i).toString() : "";
            sb.append(String.format("%-20s| %s\n", dealerCard, playerCard));
        }

        int dealerTotal = hideDealerCard ? dealer.get(1).value : handValue(dealer);
        int playerTotal = handValue(player);

        sb.append(String.format("%-20s| (Total: %d)\n", "(Total: " + dealerTotal + ")", playerTotal));
        sb.append("=============================\n");

        appendLog(sb.toString(), Color.BLACK);
    }

    // --- Helpers ---
    void appendLog(String s, Color c) {
        gameLog.setForeground(c);
        gameLog.setText(gameLog.getText() + s);
    }

    void clearLog() {
        gameLog.setText("");
    }

    // --- Main ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BlackjackCasino().setVisible(true);
        });
    }
}
