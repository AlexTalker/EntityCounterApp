import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class Trie {
    public static class TrieNode {
        boolean leaf;
        Map<Character, TrieNode> children = new TreeMap<>();

        public TrieNode getChild(Character c){
            return children.get(c);
        }

        public boolean isLeaf(){
            return leaf;
        }
    }

    TrieNode root = new TrieNode();

    public Trie(){}

    public Trie(Iterable<String> lines){
        for(String line: lines){
            put(line);
        }
    }

    public void put(String s){
        TrieNode v = root;
        for (char c: s.toCharArray()){
            if(!v.children.containsKey(c)){
                v.children.put(c, new TrieNode());
            }
            v = v.children.get(c);
        }
        v.leaf = true;
    }

    public TrieNode getRoot(){
        return root;
    }

    public static void main(String[] args){
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(new File(args[0]), new TypeReference<Map<String, Object>>() {});
            Trie trie = new Trie(map.keySet());
            for(String key: map.keySet()){
                TrieNode t = trie.root;
                for(char c: key.toCharArray()){
                    t = t.getChild(c);
                    assert t != null;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
