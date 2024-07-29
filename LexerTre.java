import java.io.*; 

public class LexerTre {

    // controllare che finisca con breakpoint

    public static int line = 1;
    private char peek = ' ';
    String id = ""; // INIZIALIZZAZIONE STRINGHE DI APPOGGIO X STRINGHE
    String num = ""; // INIZIALIZZAZIONE STRINGHE DI APPOGGIO X NUM
    
    private void readch(BufferedReader br) { // DATO DAL PROFESSORE
        try {
            peek = (char) br.read();
        } catch (IOException exc) {
            peek = (char) -1; // ERROR
        }
    }
  
    public Token lexical_scan(BufferedReader br) { // DATO DEL PROFESSORE
        
        while (peek == ' ' || peek == '\t' || peek == '\n'  || peek == '\r') {
            if (peek == '\n') line++;
            readch(br);
        }

        // ... gestire i casi di +, -, *, /, ;, (, ), {, } ... //

        switch (peek) {
            case '!':
                peek = ' ';
                return Token.not;
            case '(':
                peek = ' ';
                return Token.lpt;
            case ')':
                peek = ' ';
                return Token.rpt;
            case '[':
                peek = ' ';
                return Token.lpq;
            case ']':
                peek = ' ';
                return Token.rpq;
            case '{':
                peek = ' ';
                return Token.lpg;
            case '}':
                peek = ' ';
                return Token.rpg;
            case '+':
                peek = ' ';
                return Token.plus;
            case '-':
                peek = ' ';
                return Token.minus;
            case '*':
                peek = ' ';
                return Token.mult;
            case ',':
                peek = ' ';
                return Token.comma;
            case ';':
                peek = ' ';
                return Token.semicolon;

            case '/':

            readch(br);
            boolean control = true;

            if (peek == '/') { // 

                while (control) {

                    readch(br);

                    if (peek == '\n') { // se vai a capo esci

                        control = false;
                        peek = ' ';
                        return lexical_scan(br);

                    } else if (peek == (char)-1) { // carattere che non esiste

                        control = false;
                        return new Token(Tag.EOF);

                    }

                }

            } else if (peek == '*') {

                while (control) {

                    readch(br);
                    
                    if (peek == '*') {

                        readch(br);

                    if (peek == '/') { // vai a capo

                        control = false;
                        peek = ' ';
                        return lexical_scan(br);
                    }

                    } else if (peek == (char)-1) { // carattere inesistente

                        control = false;
                        new Token(Tag.EOF);

                    }

                }
                        
            } else {
                return Token.div;
            }
            

            case '&':

                readch(br);

                if (peek == '&') {

                    peek = ' ';
                    return Word.and;

                } else {

                    System.err.println("Erroneous character"
                            + " after & : "  + peek );
                    return null;
                }

	// ... gestire i casi di ||, <, >, <=, >=, ==, <> ... //

            case '|':
            
                readch(br);

                if(peek == '|') {

                    peek = ' ';
                    return Word.or;

                } else {

                    System.err.println("Erroneous character"
                            + " after | : "  + peek );
                    return null;
                }

            case ':':

            readch(br);

            if (peek == '=') {

                peek = ' ';
                return Word.init;

            } else {

                System.err.println("Erroneous character"
                        + " after & : "  + peek );
                return null;

            }    
                
            case '<':

                readch(br);

                if(peek == '=') {

                    peek = ' ';
                    return Word.le;

                } else if (peek == '>') {

                    peek = ' ';
                    return Word.ne;

                } else { 
                    
                    return Word.lt; 

                }

            case '>':

                readch(br);

                if(peek == '=') {

                    peek = ' ';
                    return Word.ge;

                } else { 
                    
                    return Word.gt; 
                
                }


            case (char)-1:

                return new Token(Tag.EOF);

            default:

            if (Character.isLetter(peek) | peek == '_' ) { // trovi una variabile o word essenzialmente, anche _

                while (Character.isLetter(peek) | Character.isDigit(peek) | peek == '_') {

                   id = id + peek;
                   readch(br);

                 } // finchè trova qualcosa cicla

                 String support = id;
                 id = "";

               switch (support) {

                   case "to":
                       return Word.to;
                   case "if":
                       return Word.iftok;
                   case "else":
                       return Word.elsetok;
                   case "assign":
                       return Word.assign;                    
                   case "do":
                       return Word.dotok;
                   case "for":
                       return Word.fortok;                        
                   case "begin":
                       return Word.begin;
                   case "end":
                       return Word.end;
                   case "print":
                       return Word.print;
                   case "read":
                       return Word.read;                
                   default:
                       if (support.compareTo("_")==0 && id == "") {
                           System.err.println("Erroneous character: " 
                               + '_' );
                       return null;
                       } else {
                           return new Word(Tag.ID, support);
                       }
                       
                   }
                     
                    
                }  else if (Character.isDigit(peek) | id != "") { // modifica

                    while (Character.isDigit(peek)) {
   
                   num = num + peek;
                   readch(br);
   
                   } // finchè trova qualcosa cicla
   
                   String parameter = num;
   
                   num = ""; // prima di ricominciare reinizializza la stringa
   
                   return new NumberTok(Integer.parseInt(parameter));
   
                   } else {
   
                           System.err.println("Erroneous character: " 
                                   + peek );
                           return null;
   
                   }
         }
      
    }
		
    public static void main(String[] args) {
        LexerTre lex = new LexerTre();
        String path = "C:\\Users\\Davide\\OneDrive\\Desktop\\LFTLAB\\Esercizio 2\\prova.txt"; // il percorso del file da leggere
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            Token tok;
            do {
                tok = lex.lexical_scan(br);
                System.out.println("Scan: " + tok);
            } while (tok.tag != Tag.EOF);
            br.close();
        } catch (IOException e) {e.printStackTrace();}    
    }

}