import java.util.ArrayList;
import java.util.Iterator;

public class Trie {
    private static class Node {
        public boolean isEnd;
        public int count;
        public Node[] children;

        public Node() {
            this.isEnd = false;
            this.count = 0;
            this.children = new Node[128];
        }
    }
    private final Node root;

    public Trie() {
        this.root = new Node();
    }

    public void insert(String word) {
        Node curr = root;
        for (char c : word.toCharArray()) {
            if (curr.children[c] == null) {
                curr.children[c] = new Node();
            }
            curr = curr.children[c];
            curr.count++;
        }
        curr.isEnd = true;
    }

    public ArrayList<String> getWordsWithPrefix(String prefix) {
        Node curr = root;
        for (char c : prefix.toCharArray()) {
            if (curr.children[c] == null) {
                return new ArrayList<>();
            }
            curr = curr.children[c];
        }
        ArrayList<String> result = new ArrayList<>();
        collectWords(curr, new StringBuilder(prefix), result);
        return result;
    }

    private void collectWords(Node curr, StringBuilder sb, ArrayList<String> result) {
        if (curr == null) {
            return;
        }

        if (curr.isEnd) {
            result.add(sb.toString());
        }

        for (int i = 0; i < curr.children.length; i++) {
            if (curr.children[i] != null) {
                sb.append((char) i);
                collectWords(curr.children[i], sb, result);
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }

    public int countWordsStartingWith(String prefix) {
        Node curr = root;
        for (char c : prefix.toCharArray()) {
            if (curr.children[c] == null) {
                return 0;
            }
            curr = curr.children[c];
        }
        return curr.count;
    }
}
