import java.io.*;

public class Translator {
    private Lexer lex;
    private BufferedReader pbr;
    private Token look;

    SymbolTable st = new SymbolTable();
    CodeGenerator code = new CodeGenerator();
    int count=0;

    public Translator(Lexer l, BufferedReader br) {
        lex = l;
        pbr = br;
        move();
    }

    void move() {
		look = lex.lexical_scan(pbr);
        System.out.println("token = " + look);
    }

    void error(String s) {
		throw new Error("Near line " + lex.line + ": " + s);
    }

    void match(int t) {
		if (look.tag == t) {
	        if (look.tag != Tag.EOF)
            move();
	    } else error("\nin match() metod there is a syntax error");
    }

/*Il first di prog e' come quello di stat ('(') in quanto in questo modo
  nel caso di non bilanciamento o non apertura immediata delle parentesi
  viene segnalato un errore senza ulteriori esecuzioni di altri metodi.
  Non deve però essere effettuato il match, altrimenti si causerà lo
  sbilanciamento.
*/
    public void prog() {
        switch(look.tag){
          case '(':
            int lnext_prog = code.newLabel();
            stat();
            code.emitLabel(lnext_prog);
            match(Tag.EOF);
            try {
            	code.toJasmin();
            }
            catch(java.io.IOException e) {
            	System.out.println("IO error\n");
            };
            break;
          default:
            error("\nin prog() metod there is a syntax error");
        }
    }

	private void stat() {
		switch(look.tag){
			case '(':
				match('(');
				statp();
				match(')');
				break;
			default:
				error("\nin stat() metod there is a syntax error");
		}
	}

  public void statp() {
      int lnext_true;
      int lnext_false;
      int read_id_addr;

      switch(look.tag) {
    			case '=':
    				match('=');
            read_id_addr = st.lookupAddress(((Word)look).lexeme);
            if (read_id_addr==-1) {
                read_id_addr = count;
                st.insert(((Word)look).lexeme,count++);
            }
    				match(Tag.ID);
    				expr();
            code.emit(OpCode.istore,read_id_addr);
    				break;
    		case Tag.COND:
				lnext_true = code.newLabel();
				lnext_false = code.newLabel();
				int lnext_end = code.newLabel();
				match(Tag.COND);
				bexpr(lnext_true, lnext_false);
				code.emitLabel(lnext_true);
				stat();
				elseopt(lnext_false, lnext_end);
    			break;
    		case Tag.WHILE:
				int lnext_loop = code.newLabel();
				lnext_true = code.newLabel();
				lnext_false = code.newLabel();
				match(Tag.WHILE);
				code.emitLabel(lnext_loop);
				bexpr(lnext_true, lnext_false);
				code.emitLabel(lnext_true);
				stat();
				code.emit(OpCode.GOto, lnext_loop);
				code.emitLabel(lnext_false);
				break;
			case Tag.DO:
				match(Tag.DO);
				statlist();
				break;
			case Tag.PRINT:
				match(Tag.PRINT);
				exprlist();
				code.emit(OpCode.invokestatic, 1);
				break;
          case Tag.READ:
              match(Tag.READ);
              if (look.tag==Tag.ID) {
                  read_id_addr = st.lookupAddress(((Word)look).lexeme);
                  if (read_id_addr==-1) {
                      read_id_addr = count;
                      st.insert(((Word)look).lexeme,count++);
                  }
                  match(Tag.ID);
                  code.emit(OpCode.invokestatic, 0);
                  code.emit(OpCode.istore,read_id_addr);
              }
              else
                  error("Error in grammar (stat) after read with " + look);
              break;
    			default:
    				error("\nin statp() metod there is a syntax error");
      }
   }

	private void statlist() {
		switch(look.tag){
			case '(':
        stat();
				statlistp();
				break;
			default:
				error("\nin statlist() metod there is a syntax error");
		}
	}

	private void statlistp() {
		switch(look.tag){
			case '(':
				stat();
				statlistp();
				break;
			case ')':
				break;
			default:
				error("\nin statlistp() metod there is a syntax error");
		}
	}

	private void expr() {
		switch(look.tag){
			case Tag.NUM:
        int val_NUM = ((NumberTok)look).lexeme;
				match(Tag.NUM);
        code.emit(OpCode.ldc, val_NUM);
				break;
			case Tag.ID:
        int read_id_addr = st.lookupAddress(((Word)look).lexeme);
        if(read_id_addr != -1)
          code.emit(OpCode.iload, read_id_addr);
        else
          error("Variable doesn't exist!");
        match(Tag.ID);
				break;
			case '(':
				match('(');
				exprp();
				match(')');
				break;
			default:
				error("\nin expr() metod there is a syntax error");
		}
	}

  private void exprp() {
      switch(look.tag) {
  			case '+':
  				match('+');
  				exprlist();
  				code.emit(OpCode.iadd);
  				break;
        case '-':
            match('-');
            expr();
            expr();
            code.emit(OpCode.isub);
            break;
  			case '*':
            match('*');
            expr();
            expr();
            code.emit(OpCode.imul);
            break;
  			case '/':
  				match('/');
  				exprlist();
  				code.emit(OpCode.idiv);
  				break;
  			default:
  				error("\nin exprp() metod there is a syntax error");
      }
  }

	private void elseopt(int lnext_false, int lnext_end){
		switch(look.tag){
			case '(':
				match('(');
				match(Tag.ELSE);
				code.emit(OpCode.GOto, lnext_end);
				code.emitLabel(lnext_false);
				stat();
				code.emitLabel(lnext_end);
				match(')');
				break;
			case ')':
				code.emitLabel(lnext_false);
				break;
			default:
				error("\nin elseopt() metod there is a syntax error");
		}
	}

	private void exprlist() {
		switch(look.tag){
			case Tag.NUM:
			case Tag.ID:
			case '(':
				expr();
				exprlistp();
				break;
			default:
				error("\nin exprlist() metod there is a syntax error");
		}
	}

	private void exprlistp() {
		switch(look.tag){
			case Tag.NUM:
			case Tag.ID:
			case '(':
				expr();
				exprlistp();
				break;
			case ')':
				break;
			default:
				error("\nin exprlist() metod there is a syntax error");
		}
	}

	private void bexpr(int lnext_true, int lnext_false) {
		switch(look.tag){
			case '(':
				match('(');
				bexprp(lnext_true);
				code.emit(OpCode.GOto, lnext_false);
				match(')');
				break;
			default:
				error("\nin bexpr() metod there is a syntax error");
		}
	}

	private void bexprp(int lnext_true) {
    // inizializzazione a '==' se non serve e' perché c'è un errore
    OpCode cond = OpCode.if_icmpeq;
		switch(look.tag){
			case Tag.RELOP:
        switch(((Word)look).lexeme){
          // '=='
          case "==":
            cond = OpCode.if_icmpeq;
            break;
          // '<'
          case "<":
            cond = OpCode.if_icmplt;
            break;
          // '<='
          case "<=":
            cond = OpCode.if_icmple;
            break;
          // '<>'
          case "<>":
            cond = OpCode.if_icmpne;
            break;
          // '>'
          case ">":
            cond = OpCode.if_icmpgt;
            break;
          // '>='
          case ">=":
            cond = OpCode.if_icmpge;
            break;
          default:
            error("Error in grammar (bexprp)");
        }
        match(Tag.RELOP);
				break;
		default:
			error("\nin bexprp() metod there is a syntax error");
		}
		expr();
		expr();
		code.emit(cond, lnext_true);
	}

	public static void main(String[] args) {
      Lexer lex = new Lexer();
      String path = "cond_annidati.lft";
      try {
          BufferedReader br = new BufferedReader(new FileReader(path));
          Translator traduttore = new Translator(lex, br);
          traduttore.prog();
          br.close();
      } catch (IOException e) {e.printStackTrace();}
  }
}
