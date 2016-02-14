import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.util.Map;

/**
 * Created by alx on 13.02.16.
 */
public class TokenCounter {

    private static final Trie entities;

    static{
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = null;
        try {
            map = mapper.readValue(new File("entities.json"), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        Trie trie = new Trie();
        map.keySet().forEach(s -> trie.put(s.substring(0, s.length() - 1)));
        entities = trie;
    }
    // Try make code clear
    private final char TAB = '\t';
    private final char SPACE = ' ';
    private final char LESS_THAN = '<';
    private final char AMPERSAND = '&';
    private final char SEMICOLON = ';';

    private final short ASCII_DIGIT = 0b01;
    private final short ASCII_HEX_DIGIT = 0b10;

    private long chars_counter;
    private long entities_counter;

    private final BufferedReader source;

    public TokenCounter(Reader in) throws Exception {
        source = new BufferedReader(in);
        counter();
    }

    private static boolean ZERO(int v){
        return v == 0;
    }

    private void counter() throws IOException {
        String s;
        while((s = source.readLine()) != null){
            short collect_number = 0;
            boolean segment_finished = false;
            int segment_length = 0;
            int correct_segment_length = 0;
            Trie.TrieNode t = entities.getRoot();
            for(char c: s.toCharArray()){
                switch (c){
                    case SPACE:
                    case TAB:
                    case LESS_THAN:
                    case SEMICOLON:
                    case AMPERSAND:
                        if(!ZERO(segment_length)){
                            if(c == SEMICOLON && !ZERO(correct_segment_length) && (correct_segment_length == segment_length)){
                                correct_segment_length++;
                            }
                            segment_length++;
                            segment_finished = true;
                            break;
                        }
                        else if(c == AMPERSAND){
                            t = t.getChild(AMPERSAND);
                            segment_length++;
                            break;
                        }
                    default:
                        switch (segment_length){
                            case 0:
                                chars_counter++;
                                break;
                            case 1:
                                if(c == '#'){
                                    collect_number |= ASCII_DIGIT;
                                    segment_length++;
                                    break;
                                }
                            case 2:
                                if(!ZERO(collect_number) && (c == 'x' || c == 'X')){
                                    collect_number |= ASCII_HEX_DIGIT;
                                    segment_length++;
                                    break;
                                }
                            default:
                                ++segment_length;
                                if(!ZERO(collect_number)){
                                    if(!ZERO(collect_number & ASCII_HEX_DIGIT)
                                            && (('a' <= c && c <= 'f')
                                            || ('A' <= c && c <= 'F'))){
                                            correct_segment_length = segment_length;
                                            break;
                                    }
                                    if('0' <= c && c <= '9'){
                                        correct_segment_length = segment_length;
                                    }else{
                                        segment_finished = true;
                                    }
                                }
                                else{
                                    t = t.getChild(c);
                                    segment_finished = (t == null);
                                    if(!segment_finished) {
                                        if(t.isLeaf()){
                                            correct_segment_length = segment_length;
                                        }
                                    }
                                }
                        }
                }
                if(segment_finished){
                    // No check that HEX or decimal number
                    // is out of Unicode codepoints
                    // Since anyway REPLACEMENT CHARACTER should be return
                    if(!ZERO(correct_segment_length)){
                        entities_counter++;
                    }
                    chars_counter += segment_length - correct_segment_length;
                    segment_finished = false;
                    correct_segment_length = 0;
                    segment_length = 0;
                    collect_number = 0;
                    t = entities.getRoot();
                }
            }
            chars_counter += segment_length - correct_segment_length;
            chars_counter++;// append '\n' to summary
        }
    }

    public static void main(String[] args) throws Exception {
        TokenCounter tc = new TokenCounter(new FileReader(args[0]));
        System.out.println(tc.chars_counter);
        System.out.println(tc.entities_counter);
    }
}
