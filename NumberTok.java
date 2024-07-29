public class NumberTok extends Token {
    int lexeme = 0;
    public NumberTok(int s) { super(Tag.NUM); lexeme = s; }  // ti fai passare un valore dal programma
    public String toString() { return "<" + Tag.NUM + ", " + lexeme + ">"; }
    
}