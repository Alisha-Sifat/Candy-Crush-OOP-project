import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Game {
    final int SIZE = 8;
    final int PIXEL_SIZE = 600;
    final int SQUARE_SIZE = PIXEL_SIZE/SIZE;
    int score = 0;
    Image BG;
    Image SELECTOR;
    Image[] CANDY = new Image[6];
    Random random = new Random();
    Point selectedPiece = null;
    int[][] board = new int[SIZE][SIZE];
    JFrame frame = new JFrame("Score: 0");
    JPanel panel;
    public void run() throws IOException{
        // loading images
        BG =ImageIO.read(getClass().getResource("/images/b.png"));
        SELECTOR = ImageIO.read(getClass().getResource("/images/s.png"));
        for (int y = 1;y<=CANDY.length;y++) {
            CANDY[y-1] = ImageIO.read(
    getClass().getResource("/images/" + y + ".png")
);
        }
        fill(board, -1);
        initFill();
        panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                render(g);
            }
            
        };
        panel.setPreferredSize(new Dimension(PIXEL_SIZE,PIXEL_SIZE));
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            handleClick(e);
            panel.repaint();
            }
            
        });
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    private void render(Graphics g){
            g.drawImage(BG, 0  , 0, PIXEL_SIZE,PIXEL_SIZE, frame);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect( 0  , 0, PIXEL_SIZE,PIXEL_SIZE);
                for(int y = 0;y<SIZE;y++){
                for(int x = 0;x<SIZE;x++){
                    g.drawRect(x*SQUARE_SIZE, y*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                    if(board[y][x]>=0){
                    g.drawImage(CANDY[board[y][x]], x*SQUARE_SIZE, y*SQUARE_SIZE,SQUARE_SIZE,SQUARE_SIZE, frame);
                }
            }  
                if(selectedPiece!=null){
                    g.drawImage(SELECTOR, selectedPiece.x*SQUARE_SIZE, selectedPiece.y*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE ,frame);
                } 
                }
    }
    private void handleClick(MouseEvent e){
        int x = e.getX()/SQUARE_SIZE;
        int y = e.getY()/SQUARE_SIZE;
        if(board[y][x]>=0){
       // checks if the selected square is a neighbor square
                if(selectedPiece!=null&&((Math.abs(x-selectedPiece.x)==1&&y==selectedPiece.y)||(Math.abs(y-selectedPiece.y)==1&&x==selectedPiece.x))){
                replace(selectedPiece, new Point(x,y));
                ArrayList<Point> matches = matches();
                if(matches.isEmpty()){
                replace(selectedPiece, new Point(x,y));
                }else{
                while(!matches.isEmpty()) {
                remove(matches);
                gravity();
                matches = matches();
                }
                refill();
                }
               
                
                selectedPiece = null;
                }else{
                    selectedPiece = new Point(x,y);
                }
            }
        
    }
    private ArrayList<Point> matches(){
        ArrayList<Point> result = new ArrayList<>();
            int countLine = 1;
            int countCol = 1;
        for(int y = 0;y<SIZE;y++){
            for(int x = 1;x<SIZE;x++){
                if (board[y][x]!=-1&&(board[y][x]==board[y][x-1])) {
                    countLine++;
                }
                if (board[y][x]!=board[y][x-1]||x==SIZE-1) {
                    if(countLine>=3){
                    countLine-=board[y][x]==board[y][x-1]?1:0;
                    for(int i=board[y][x]==board[y][x-1]?0:1;i<=countLine;i++){
                           result.add(new Point(x-i,y));
                    }
                    }
                    countLine=1;
                }
            
           
                if (board[x][y]!=-1&&(board[x][y]==board[x-1][y])) {
                    countCol++;
                }
                if (board[x][y]!=board[x-1][y]||x==SIZE-1) {
                    if(countCol>=3){
                    countCol-=board[x][y]==board[x-1][y]?1:0;
                    for(int i=board[x][y]==board[x-1][y]?0:1;i<=countCol;i++){
                           result.add(new Point(y,x-i));
                    }
                    }
                     countCol=1;
                }
               
            
        }
    }
        return result;
    }
    private void initFill(){
        for(int y = 0;y<SIZE;y++){
            for(int x =0;x<SIZE;x++){
                board[y][x] = random.nextInt(CANDY.length);
                while(!matches().isEmpty()){
                   board[y][x] = random.nextInt(CANDY.length); 
                }
            }
        }
    }
    private void refill(){
        for(int y = 0;y<SIZE;y++){
            for(int x =0;x<SIZE;x++){
                if(board[y][x]==-1){
                board[y][x] = random.nextInt(CANDY.length);
                while(!matches().isEmpty()){
                   board[y][x] = random.nextInt(CANDY.length); 
                }
            }
            }
        }
    }
    private void gravity(){
        boolean replaced = false;
        for(int y = 0;y<SIZE;y++){
            for(int x =0;x<SIZE;x++){
                if (board[y][x]==-1&&y-1>=0&&board[y-1][x]!=-1) {
                    board[y][x]=board[y-1][x];
                    board[y-1][x]=-1;
                    replaced = true;
                }
            }
        }
        if(replaced){
            gravity();
        }
    }
    private void remove(ArrayList<Point> toRemove){
        for (Point p : toRemove) {
            board[p.y][p.x] = -1;
            score++;
            frame.setTitle("Score: "+score);
        }
    }
    private void replace(Point a, Point b){
        int temp = board[a.y][a.x];
        board[a.y][a.x] = board[b.y][b.x];
        board[b.y][b.x] = temp;
    }
    private void fill(int[][] board, int i){
                 for(int y = 0;y<SIZE;y++){
                for(int x = 0;x<SIZE;x++){
                    board[y][x]=i;
                }
            }
    }
}