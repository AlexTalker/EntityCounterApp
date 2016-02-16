import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.net.URI;
import java.io.*;
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
    private final static char TAB = '\t';
    private final static char SPACE = ' ';
    private final static char LESS_THAN = '<';
    private final static char GREATHER_THAN = '>';
    private final static char AMPERSAND = '&';
    private final static char SEMICOLON = ';';

    private final static short ASCII_DIGIT = 0b01;
    private final static short ASCII_HEX_DIGIT = 0b10;

    private enum STATES{
        DATA, OPEN_TAG,// TODO: <!-- ... -->
        BEFORE_ATTRIBUTE, ATTRIBUTE,
        DOUBLE_QUOTES, SINGLE_QUOTES,
        ATTRIBUTE_VALUE, CLOSING_TAG
    };

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
            STATES state = STATES.DATA;
            for(char c: s.toCharArray()){
                state = switch_state(state, c);
                switch (c){
                    case SPACE:
                    case TAB:
                    case LESS_THAN:
                    case SEMICOLON:
                    case AMPERSAND:
                        if(!ZERO(segment_length)){
                            if(c == SEMICOLON && !ZERO(correct_segment_length) && (correct_segment_length == segment_length)) {
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
                        switch (state){
                            case ATTRIBUTE_VALUE:
                            case DOUBLE_QUOTES:
                            case SINGLE_QUOTES:
                                if(c != SEMICOLON) {
                                    correct_segment_length = 0;
                                    break;
                                }
                            default:
                                entities_counter++;
                        }
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

    private static STATES switch_state(STATES previous, char c){
        STATES next = previous;
        switch (c){
            case TAB:
            case SPACE:
                switch (previous){
                    case ATTRIBUTE_VALUE:
                    case OPEN_TAG:
                        next = STATES.BEFORE_ATTRIBUTE;
                }
                break;
            case LESS_THAN:
                if(previous == STATES.DATA){
                    next = STATES.OPEN_TAG;
                }
                break;
            case GREATHER_THAN:
                switch (previous){
                    case BEFORE_ATTRIBUTE: // DIRTY!!!
                    case OPEN_TAG:
                    case CLOSING_TAG:
                    case ATTRIBUTE_VALUE:
                        next = STATES.DATA;
                }
                break;
            case '/':
                switch (previous){
                    case OPEN_TAG:
                    case BEFORE_ATTRIBUTE:
                        next = STATES.CLOSING_TAG;
                }
                break;
            case '=':
                if(previous == STATES.ATTRIBUTE){
                    next = STATES.ATTRIBUTE_VALUE;
                }
                break;
            case '"': // fix '
                switch (previous){
                    case ATTRIBUTE_VALUE:
                        next = STATES.DOUBLE_QUOTES;
                    case DOUBLE_QUOTES:
                        next = STATES.ATTRIBUTE_VALUE;
                }
                break;
            case '\'':
                switch (previous){
                    case ATTRIBUTE_VALUE:
                        next = STATES.SINGLE_QUOTES;
                    case SINGLE_QUOTES:
                        next = STATES.ATTRIBUTE_VALUE;
                }
                break;
            default:
                if((('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9'))
                        && previous == STATES.BEFORE_ATTRIBUTE){
                    next = STATES.ATTRIBUTE;
                }
        }
        return next;
    }

    public static void main(String[] args) throws Exception {
        URL url = new File(args[0]).toURI().toURL();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        TokenCounter tc = new TokenCounter(in);
        System.out.println(tc.chars_counter);
        System.out.println(tc.entities_counter);
    }
}
