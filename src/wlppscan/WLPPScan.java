/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package wlppscan;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.*;
import java.math.*;

/** A sample main class demonstrating the use of the Lexer.
 * This main class just outputs each line in the input, followed by
 * the tokens returned by the lexer for that line.
 *
 * @version 071011.0
 */
public class WLPPScan {
    public static final void main(String[] args) {
        new WLPPScan().run();
    }

    private Lexer lexer = new Lexer();

    private void run() {
        Scanner in = new Scanner(System.in);
        while(in.hasNextLine()) {
            String line = in.nextLine();

            // Scan the line into an array of tokens.
            Token[] tokens;
            tokens = lexer.scan(line);

            // Print the tokens produced by the scanner
            for( int i = 0; i < tokens.length; i++ ) {
                //System.out.println(tokens[i]);
                String x = tokens[i].toString();
                StringTokenizer tok = new StringTokenizer(x);
                System.out.print(tok.nextToken());
                x = tok.nextToken();
                System.out.println(" " + x.substring(1,x.length() -1));
            }
        }
        System.out.flush();
    }
}
//Chaning a little bit to understand how git works :)
/** The various kinds of tokens. */
enum Kind {
    ID,
    NUM,
    LBRACK,
    RBRACK,
    LBRACE,
    RBRACE,
    LPAREN,
    RPAREN,
    GT,
    LT,
    PLUS,
    MINUS,
    PCT,
    STAR,
    SLASH,
    SEMI,
    COMMA,
    AMP,
    BECOMES,
    ZERO,
    COMMENT,
    NE,
    LE,
    GE,
    EQ,
    ERR,
    WHITESPACE,RETURN, IF, ELSE, WHILE, PRINTLN, WAIN, INT, NEW, NULL, DELETE;
}

/** Representation of a token. */
class Token {
    public Kind kind;     // The kind of token.
    public String lexeme; // String representation of the actual token in the
                          // source code.

    public Token(Kind kind, String lexeme) {
        this.kind = kind;
        this.lexeme = lexeme;
    }
    public String toString() {
        return kind+" {"+lexeme+"}";
    }
   
    private int parseLiteral(String s, int base, int bits) {
        BigInteger x = new BigInteger(s, base);
        if(x.signum() > 0) {
            if(x.bitLength() > bits) {
                System.err.println("ERROR in parsing: constant out of range: "+s);
                System.exit(1);
            }
        } else if(x.signum() < 0) {
            if(x.negate().bitLength() > bits-1
            && x.negate().subtract(new BigInteger("1")).bitLength() > bits-1) {
                System.err.println("ERROR in parsing: constant out of range: "+s);
                System.exit(1);
            }
        }
        return (int) (x.longValue() & ((1L << bits) - 1));
    }
}

/** Lexer -- reads an input line, and partitions it into a list of tokens. */
class Lexer {
    public Lexer() {
        CharSet whitespace = new Chars("\t\n\r ");
        CharSet letters = new Chars(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        CharSet lettersDigits = new Chars(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        CharSet digits = new Chars("0123456789");
        CharSet hexDigits = new Chars("0123456789ABCDEFabcdef");
        CharSet oneToNine = new Chars("123456789");
        CharSet symbols = new Chars("!&%*+()/,-;>=<[]{}");
        CharSet all = new AllChars(); 

        table = new Transition[] {
                new Transition(State.START, whitespace, State.WHITESPACE),
                new Transition(State.START, letters, State.ID),
                new Transition(State.ID, lettersDigits, State.ID),
                new Transition(State.START, oneToNine, State.NUM),
                new Transition(State.ZERO, oneToNine, State.ERR),
                new Transition(State.ZERO, letters, State.ERR),
                new Transition(State.ERR, all, State.ERR),
                new Transition(State.NUM, digits, State.NUM),
                new Transition(State.NUM, letters, State.ERR),
                new Transition(State.START, new Chars("-"), State.MINUS),
                new Transition(State.START, new Chars(","), State.COMMA),
                new Transition(State.START, new Chars("/"), State.SLASH),
                new Transition(State.START, new Chars("&"), State.AMP),
                new Transition(State.START, new Chars("*"), State.STAR),
                new Transition(State.START, new Chars("+"), State.PLUS),
                new Transition(State.START, new Chars("%"), State.PCT),
                new Transition(State.START, new Chars(";"), State.SEMI),
                new Transition(State.START, new Chars("{"), State.LBRACE),
                new Transition(State.START, new Chars("}"), State.RBRACE),
                new Transition(State.START, new Chars("["), State.LBRACK),
                new Transition(State.START, new Chars("]"), State.RBRACK),
                new Transition(State.START, new Chars("("), State.LPAREN),
                new Transition(State.START, new Chars(")"), State.RPAREN),
                new Transition(State.START, new Chars("0"), State.ZERO),
                new Transition(State.START, new Chars("!"), State.EXCLAIM),
                new Transition(State.EXCLAIM, new Chars("="), State.NE),
                new Transition(State.START, new Chars("="), State.BECOMES),
                new Transition(State.BECOMES, new Chars("="), State.EQ),
                new Transition(State.START, new Chars(">"), State.GT),
                new Transition(State.GT, new Chars("="), State.GE),
                new Transition(State.START, new Chars("<"), State.LT),
                new Transition(State.LT, new Chars("="), State.LE),
                new Transition(State.SLASH, new Chars("/"), State.COMMENT),
                new Transition(State.COMMENT, all, State.COMMENT),
                new Transition(State.COMMENT, all, State.COMMENT)
                
        };
    }
    /** Partitions the line passed in as input into an array of tokens.
     * The array of tokens is returned.
     */
    public Token[] scan( String input ) {
        List<Token> ret = new ArrayList<Token>();
        if(input.length() == 0) return new Token[0];
        int i = 0;
        int startIndex = 0;
        State state = State.START;
        while(true) {
            Transition t = null;
            if(i < input.length()) t = findTransition(state, input.charAt(i));
            if(t == null) {
                // no more transitions possible
                if(!state.isFinal()) {
                    System.err.println("ERROR in lexing after reading "+input.substring(0, i));
                    System.exit(1);
                }
                if( state.kind != Kind.WHITESPACE ) {
                    if(state.kind == Kind.ID){
                        if(input.substring(startIndex, i).equals("int")){
                            ret.add(new Token(Kind.INT,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("NULL")){
                            ret.add(new Token(Kind.NULL,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("wain")){
                            ret.add(new Token(Kind.WAIN,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("delete")){
                            ret.add(new Token(Kind.DELETE,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("new")){
                            ret.add(new Token(Kind.NEW,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("return")){
                            ret.add(new Token(Kind.RETURN,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("if")){
                            ret.add(new Token(Kind.IF,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("else")){
                            ret.add(new Token(Kind.ELSE,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("println")){
                            ret.add(new Token(Kind.PRINTLN,
                                input.substring(startIndex, i)));
                        }
                        else if(input.substring(startIndex, i).equals("while")){
                            ret.add(new Token(Kind.WHILE,
                                input.substring(startIndex, i)));
                        }
                        else{
                            ret.add(new Token(state.kind,
                                input.substring(startIndex, i)));
                        }
                    }
                    else if(state.kind == Kind.ZERO){
                        ret.add(new Token(Kind.NUM,
                                input.substring(startIndex, i)));
                    }
                    else if(state.kind == Kind.ERR){
                        System.err.println("ERROR in lexing while reading "+
                                input.substring(0, i));
                        System.exit(2);
                    }
                    else{
                    ret.add(new Token(state.kind,
                                input.substring(startIndex, i)));}
                }
                startIndex = i;
                state = State.START;
                if(i >= input.length()) break;
            } else {
                state = t.toState;
                i++;
            }
        }
        return ret.toArray(new Token[ret.size()]);
    }

    ///////////////////////////////////////////////////////////////
    // END OF PUBLIC METHODS
    ///////////////////////////////////////////////////////////////

    private Transition findTransition(State state, char c) {
        for( int j = 0; j < table.length; j++ ) {
            Transition t = table[j];
            if(t.fromState == state && t.chars.contains(c)) {
                return t;
            }
        }
        return null;
    }

    private static enum State {
        START(null),
        MINUS(Kind.MINUS),
        NUM(Kind.NUM),
        ID(Kind.ID),
        COMMA(Kind.COMMA),
        LPAREN(Kind.LPAREN),
        RPAREN(Kind.RPAREN),
        ZERO(Kind.ZERO),
        COMMENT(Kind.WHITESPACE),
        WHITESPACE(Kind.WHITESPACE),
        EXCLAIM(null),
        AMP(Kind.AMP),
        PCT(Kind.PCT),
        STAR(Kind.STAR),
        PLUS(Kind.PLUS),
        LBRACK(Kind.LBRACK),
        RBRACK(Kind.RBRACK),
        LBRACE(Kind.LBRACE),
        RBRACE(Kind.RBRACE),
        SLASH(Kind.SLASH),
        SEMI(Kind.SEMI),
        GT(Kind.GT),
        LT(Kind.LT),
        BECOMES(Kind.BECOMES),
        NE(Kind.NE),
        EQ(Kind.EQ),
        LE(Kind.LE),
        GE(Kind.GE),
        RETURN(null),
        IF(null),
        ELSE(null),
        WHILE(null),
        PRINTLN(null),
        WAIN(null),
        INT(null),
        NEW(null),
        NULL(null),
        DELETE(null),
        ERR(Kind.ERR);
        State(Kind kind) {
            this.kind = kind;
        }
        Kind kind;
        boolean isFinal() {
            return kind != null;
        }
    }

    private interface CharSet {
        public boolean contains(char newC);
    }
    private class Chars implements CharSet {
        private String chars;
        public Chars(String chars) { this.chars = chars; }
        public boolean contains(char newC) {
            return chars.indexOf(newC) >= 0;
        }
    }
    private class AllChars implements CharSet {
        public boolean contains(char newC) {
            return true;
        }
    }

    private class Transition {
        State fromState;
        CharSet chars;
        State toState;
        Transition(State fromState, CharSet chars, State toState) {
            this.fromState = fromState;
            this.chars = chars;
            this.toState = toState;
        }
    }
    private Transition[] table;
}
