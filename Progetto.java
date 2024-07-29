 import java.io.*;
 
 public class Progetto {
   
    private LexerTre lex; // qui dichiariamo il lexere del main
    private BufferedReader pbr;
    private Token look;

    SymbolTable st = new SymbolTable();
    CodeGenerator code = new CodeGenerator();
    int count=0;


    public Progetto(LexerTre l, BufferedReader br) {
        lex = l;
        pbr = br;
        move();
    }

    void move() { // idea == parser avanza al prossimo token da leggere nella sequenza

        look = lex.lexical_scan(pbr); // ogni chiamata chiama il lexical scan
        System.out.println("token = " + look);
    }

    void error(String s) { // in argomento ha una stringa e da in output un mex di errore
	throw new Error("near line " + LexerTre.line + ": " + s);
    }

    void match(int t) { // prende un int in argomento e fa confronto tra int e il nome del token che stiamo analizzando

	if (look.tag == t) { // look.tag è il nome del tag che stiamo analizzando

	    if (look.tag != Tag.EOF) move(); // se non siamo davanti all'ultimo elemento allora andiamo avanti

	} else error("syntax error");

    }

    public void prog() { // Scrivi come variabili d'accesso l'insieme guida della prima variabile che trovi

	if (look.tag == Tag.ASSIGN || look.tag == Tag.PRINT | look.tag == Tag.READ | look.tag == Tag.FOR| look.tag == '{' | look.tag == Tag.IF) {

        int [] lnext_prog = new int[5];  // ritorno l'etichetta (ereditato)
        lnext_prog[0] = code.newLabel(); // sovrascribile, statlist non ritorna nulla, in prog() lnext[0]=0 SEMPRE
        statlist(lnext_prog);
        code.emit(OpCode.GOto, lnext_prog[0]);
        code.emitLabel(lnext_prog[0]);
        match(Tag.EOF);
        try {
        	code.toJasmin();
        }
        catch(java.io.IOException e) {
        	System.out.println("IO error\n");
        };
    } else error("Errore su prog()");

    }

    public void statlist(int [] lnext) { // valore corrente etichetta

        if (look.tag == Tag.ASSIGN || look.tag == Tag.PRINT | look.tag == Tag.READ | look.tag == Tag.IF| look.tag == Tag.FOR| look.tag == '{') { 
            
            stat(lnext); 
            statlistp(lnext); 
            
        } else error("Errore su statlist()");

    }

    public void statlistp(int [] id_addr) {

        switch (look.tag) {
            case ';':
            move();
            stat(id_addr);
            statlistp(id_addr);
            break;

            case '}':
            break;

            case Tag.EOF:
            break;
        
            default: error("Errore su statlistp()");
        }
        
    }

    public void stat(int [] array_label) {

        int label = code.newLabel();

        switch(look.tag) {

            case Tag.ASSIGN:
            move();
            assignlist();
            array_label[4]= label; // mi salvo nello slot jolly l'etichetta successiva
            code.emit(OpCode.GOto, array_label[4]);
            code.emitLabel(array_label[4]);
            break;

            case Tag.PRINT:
            move();
            match('(');
            exprlist(1, 0, null);
            code.emit(OpCode.invokestatic, 1);
            match(')');
            break;

            case Tag.READ:
            move();
            match('(');
            code.emit(OpCode.invokestatic, 0); // con operand==1 fa print
            idlist(0);
            code.emit(OpCode.GOto, label);
            code.emitLabel(label); // vai all'istruzione immediatamente successiva
            match(')');
            break;

            case Tag.FOR:  // for (K 
            move();
            int return_id_for = label;
            match('(');
            code.emit(OpCode.GOto, return_id_for); // isolo l'istruzione su cui poi dovrò ciclare
            code.emitLabel(return_id_for);
            array_label[4] = return_id_for; // mi salvo l'indirizzo di ritorno
            recover(array_label);
            break;

            case Tag.IF:
            move();
            match('(');
            int return_id_if = array_label[0];

            // array_label[0] e array_label[1] sono TRUE e FALSE, 
            //quindi mi salvo il prossimo indirizzo e lo sposto su array_label[3]   

            array_label = bexpr();
            array_label[3]= return_id_if; 

            // in array_label[3] mantengo sempre l'indirizzo di ritorno

            match(')');
            array_label[2] = code.newLabel();
            stat(array_label);
            recovertwo(array_label);
            break;

            case '{':
            move();
            statlist(array_label);
            match('}');
            break;

            default:
            error("Errore su stat()");

        }

    }

    public void recover(int [] gestione_for) {

        switch(look.tag){
            case Tag.ID:
            
                int id_addr = st.lookupAddress(((Word)look).lexeme); // prende indirizzo della parola
                if (id_addr==-1) {
                    id_addr = count;
                    st.insert(((Word)look).lexeme,count++);
                } // Lo uso per fare lo store, controlla che ci siano le variabili e me le salva su id_addr

                move();
                match(Word.init.tag);
                expr();
                code.emit(OpCode.istore, id_addr);
                match(';');
                int label_cui_tornare = code.newLabel();
                code.emit(OpCode.GOto, label_cui_tornare); // go to prossima immediata
                code.emitLabel(label_cui_tornare);

                //INIZIO GESTIONE CICLO

                gestione_for = bexpr();
                gestione_for[3] = label_cui_tornare;

                // array_label[0] e array_label[1] sono TRUE e FALSE, 
                //quindi mi salvo il prossimo indirizzo e lo sposto su array_label[3]
                // in array_label[3] mantengo sempre l'indirizzo di ritorno
 
                int id_addr_false = gestione_for[1];
                match(')');
                match(Tag.DO);
                stat(gestione_for);
                code.emit(OpCode.GOto, gestione_for[3]);
                code.emitLabel(id_addr_false);
                break;

            case Tag.RELOP:
                int ricomincio_ciclo = gestione_for[4]; // uso il 4 come jolly per salvarmi gli indirizzi
                gestione_for = bexpr();
                gestione_for[3] = ricomincio_ciclo;
                match(')');
                match(Tag.DO);
                stat(gestione_for);
                code.emit(OpCode.GOto, gestione_for[3]);
                code.emitLabel(gestione_for[1]);
                break;

            default:
                error("error in temp");
        }

    }

    public void recovertwo(int [] if_else) {

        int id_addr_false = if_else[1]; 
        int id_fine_else = if_else[2];

        switch (look.tag) {

            case Tag.ELSE:
            move();
            code.emit(OpCode.GOto, id_fine_else);  
            code.emitLabel(id_addr_false); 
            stat(if_else);
            code.emit(OpCode.GOto, id_fine_else);  
            code.emitLabel(id_fine_else); // controlla che 
            match(Tag.END);
            break;

            case Tag.END:
            move();            
            code.emit(OpCode.GOto, id_addr_false);  
            code.emitLabel(id_addr_false); 
            break;


            default:
                error("Errore su recovertwo()");
        }

    }

    public void assignlist() {

        match('[');
        expr();
        match(Tag.TO);
        code.emit(OpCode.dup);
        idlist(1);
        code.emit(OpCode.pop);
        match(']');
        assignlistp();

    }

    public void assignlistp() {

        if (look.tag == '[') {
            move();
            expr();
            match(Tag.TO);
            code.emit(OpCode.dup);
            idlist(1);
            code.emit(OpCode.pop);
            match(']');
            assignlistp();

        } else if (look.tag == Tag.END | look.tag == Tag.ELSE | look.tag == ';' | look.tag == Tag.EOF || look.tag == '}') {} else error("Errore su assignlistp()");

    }

    public void idlist(int cnt_read_assign) {

        switch(look.tag) {

            case Tag.ID:
                int id_addr = st.lookupAddress(((Word)look).lexeme);
                    if (id_addr==-1) {
                        id_addr = count;
                        st.insert(((Word)look).lexeme,count++);
                    }

                    code.emit(OpCode.istore, id_addr);
                    match(Tag.ID);
                    idlistp(cnt_read_assign);
                    break;

            default: error("Errore su idlist()");
            
        }

    }

    public void idlistp(int cnt_read_assign) {
    
    if (look.tag == ',') {

        match(',');

        int id_addr = st.lookupAddress(((Word)look).lexeme);
            if (id_addr==-1) {
                id_addr = count;
                st.insert(((Word)look).lexeme,count++);
            }

        if (cnt_read_assign==0) { // sono in read

            code.emit(OpCode.invokestatic, 0); 
            code.emit(OpCode.istore, id_addr); // salvo il valore di variabile e store

        } else { // entro con assign

            code.emit(OpCode.dup);
            code.emit(OpCode.istore, id_addr); // salvo il valore di variabile e store

        }
        match(Tag.ID);
        idlistp(cnt_read_assign);

    } else if (look.tag == ')' | look.tag == ']') {} else error("Errore su idlistp()");

    }
    
    public int[] bexpr() {

        String relop = ((Word)look).lexeme;
        match(Tag.RELOP);
        int array_label[] = new int[5];
        array_label[0] = code.newLabel(); // true
        array_label[1] = code.newLabel(); // false
        int id_addr_true = array_label[0];
        int id_addr_false = array_label[1];
        expr();
        expr();

        switch (relop) {
            case "<=":
                code.emit(OpCode.if_icmple, id_addr_true);                
                break;

            case ">=":
                code.emit(OpCode.if_icmpge, id_addr_true);
                break;

            case ">":
                code.emit(OpCode.if_icmpgt, id_addr_true);
                break;

            case "<":
                code.emit(OpCode.if_icmplt, id_addr_true);
                break;

            case "<>":
                code.emit(OpCode.if_icmpne, id_addr_true);
                break;

            case "==":
                code.emit(OpCode.if_icmpeq, id_addr_true);
                break;
        
            default:
                break;
        }

        code.emit(OpCode.GOto, id_addr_false);  // cambia nome 
        code.emitLabel(id_addr_true);

        return array_label;

    }

    public void expr() { // va messa una nuova variabile che controlli le doppie add e store

        switch(look.tag){
            case '+':
                move();
                match('(');
                exprlist(0, 0, OpCode.iadd); // passarmi Opcode iadd per somme multiple
                match(')');
                code.emit(OpCode.iadd);
                break;

            case '-':
                move();
                expr();
                expr();
                code.emit(OpCode.isub);
                break;

            case '*':
                move();
                match('(');
                exprlist(0, 0, OpCode.imul); // RICONTROLLA
                match(')');
                code.emit(OpCode.imul);
                break;

            case '/':
                move();
                expr();
                expr();
                code.emit(OpCode.idiv);
                break;

            case Tag.NUM:
                code.emit(OpCode.ldc, ((NumberTok)look).lexeme);
                move();
                break;

            case Tag.ID:
            int id_addr = st.lookupAddress(((Word)look).lexeme);
                    if (id_addr==-1) {
                        id_addr = count;
                        st.insert(((Word)look).lexeme,count++);
                    }
                code.emit(OpCode.iload, id_addr); // controlla che variabile salvata e iload
                move();
                break;

            default:
                error("Errore in expr()");
        }

    }

    public void exprlist(int cnt_print_multiple, int cnt_sum_assign, OpCode val) {

        if (look.tag == '+'| look.tag == '-' |look.tag == '*' |look.tag == '/' | look.tag == Tag.ID |look.tag == Tag.NUM) {

            expr();
            exprlistp(cnt_print_multiple, cnt_sum_assign, val);

        } else error("Errore su exprlist()");

    }

    public void exprlistp(int cnt_print_multiple, int cnt_sum_mul_assign, OpCode val) {

        switch (look.tag) {

        case ',':
        move(); 
        if (cnt_print_multiple==1) {
            code.emit(OpCode.invokestatic, 1);
        } else if (cnt_sum_mul_assign >= 1 && (val == OpCode.iadd || val == OpCode.imul)) {
            code.emit(val);
        }
        expr();
        cnt_sum_mul_assign++;
        exprlistp(cnt_print_multiple, cnt_sum_mul_assign, val);
        break;        
        
        case ')':
        break;

        default: error("Errore su exprlistp()");
        }

    }

    public static void main(String[] args) {
        LexerTre lex = new LexerTre(); // alla fine analisi lessicale --> analisi semantica
        
        String path = "C:\\Users\\Davide\\OneDrive\\Desktop\\LFTLAB\\Esercizio 5\\prova.txt"; // il percorso del file da leggere

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

            Progetto parser = new Progetto(lex, br);

            parser.prog(); // entra nel metodo, se da errore essendo un try/catch esce e chatca l'errore

            System.out.println("Input OK");

            br.close();
        } catch (IOException e) {e.printStackTrace();}
    }
}
