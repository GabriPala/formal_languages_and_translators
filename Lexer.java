import java.io.*;
import java.util.*;

public class Lexer {

    public static int line = 1;
    private char peek = ' ';

    private void readch(BufferedReader br) {
        try {
            peek = (char) br.read();
        } catch (IOException exc) {
            peek = (char) -1; // ERROR
        }
    }

    public Token lexical_scan(BufferedReader br) {
        while (peek == ' ' || peek == '\t' || peek == '\n'  || peek == '\r' || peek == '/') {
            if (peek == '\n' || peek == '\r')
              line++;

			/** INIZIO DFA RICONOSCIMENTO COMMENTI (e simbolo divisione) **/
			int state = 0;

			while (state >= 0) {

				switch (state) {
				case 0:
					if (peek == '/'){
						state = 1;
						readch(br);
					}
					else
						state = -1;
					break;

				case 1:
					if (peek == '/'){
						state = 2;
						readch(br);
					}
					else if (peek == '*'){
						state = 3;
						readch(br);
					}
					else
						state = 4;		// stato finale
					break;

				case 2:
					if(peek == '\n' || peek == '\r')
						state = 5;		// stato finale
					else{
							state = 2;
							readch(br);
					}
					break;

				case 3:
					if (peek == '*')
						state = 6;
					else
						state = 3;
					readch(br);
					break;

				// stato finale
				case 4:
					return Token.div;

				// stati finali
				case 5:
				case 7:
					state = -1;
					break;

				case 6:
					if (peek == '*'){
						state = 6;
						readch(br);
					}
					else if (peek == '/')
						state = 7;		// stato finale
					else{
						state = 3;
						readch(br);
					}
					break;
				}
			}
			/** FINE DFA RICONOSCIMENTO COMMENTI (e simbolo divisione) **/
			readch(br);
		}

    switch (peek) {
      case '!':
          peek = ' ';
          return Token.not;

		/** INIZIO GESTIONE CASI (, ), {, }, +, -, *, /, ; **/

			case '(':
                peek = ' ';
                return Token.lpt;

			case ')':
                peek = ' ';
                return Token.rpt;

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

			case ';':
                peek = ' ';
                return Token.semicolon;

			/** FINE GESTIONE CASI (, ), {, }, +, -, *, /, ; **/

            case '&':
                readch(br);
                if (peek == '&') {
                    peek = ' ';
                    return Word.and;
                } else {
                    System.err.println("Erroneous character" + " after & : "  + peek );
                    return null;
                }

			/** INIZIO GESTIONE CASI ||, <, >, <=, >=, ==, <>, = **/

			case '|':
                readch(br);
                if (peek == '|') {
                    peek = ' ';
                    return Word.or;
                } else {
                    System.err.println("Erroneous character" + " after | : "  + peek );
                    return null;
                }

			case '=':
                readch(br);
                if (peek == '=') {
                    peek = ' ';
                    return Word.eq;
                } else
					           return Token.assign;

			case '<':
                readch(br);
                if (peek == '=') {
                    peek = ' ';
                    return Word.le;
                } else if(peek == '>') {
                    peek = ' ';
                    return Word.ne;
                }else
					           return Word.lt;

			case '>':
                readch(br);
                if (peek == '=') {
                    peek = ' ';
                    return Word.ge;
                } else
					          return Word.gt;

			/** FINE GESTIONE CASI ||, <, >, <=, >=, ==, <>, = **/

            case (char)-1:
                return new Token(Tag.EOF);

            default:
				// GESTIONE CASO DEGLI IDENTIFICATORI E DELLE PAROLE CHIAVE
                if (Character.isLetter(peek) || peek == '_') {
					String s = "";

                    do{
						s = s + peek;
						readch(br);
                    }while(Character.isLetter(peek) || peek=='_' || Character.isDigit(peek));

                    switch(s){
                      case "cond":
                            return Word.cond;
                      case "when":
                            return Word.when;
                      case "then":
                            return Word.then;
                      case "else":
                            return Word.elsetok;
                      case "while":
                            return Word.whiletok;
                      case "do":
                            return Word.dotok;
                      case "seq":
                            return Word.seq;
                      case "print":
                            return Word.print;
                      case "read":
                            return Word.read;
                      default:
						  boolean onlyUnderscore = true;
						for(int i = 0; i < s.length() && onlyUnderscore; i++)
							onlyUnderscore = (s.charAt(i) == '_');		// controllo che la stringa non sia formata solo da '_'

						if(onlyUnderscore){
							System.out.println("Erroneous string: " + s );
							return null;
						}
						else
							return new Word(Tag.ID, s);
                    }
                }
				// GESTIONE CASO DEI NUMERI
				else if (Character.isDigit(peek)) {
          			String s = "";
                    if(peek=='0')
                    {
                      s = s + peek;
                      readch(br);
                      if(Character.isDigit(peek)){
                        System.out.println("Erroneous string: " + s );
                        return null;
                     }
                     else
                        return new NumberTok(Tag.NUM, Integer.valueOf(s));
                    }
                    else
                    {
						do{
							s = s + peek;
							readch(br);
                        }while(Character.isDigit(peek));
						return new NumberTok(Tag.NUM, Integer.valueOf(s));
                    }
                }
                else {
                        System.err.println("Erroneous character: " + peek );
                        return null;
                }
			}
	}

    public static void main(String[] args) {
        Lexer lex = new Lexer();
        String path = "FileDaLeggere.txt"; 	// percorso del file da leggere
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
