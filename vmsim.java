/*


Outline:
- vmsim class - handle parameters and call appropriate algorithm
- PageTable class - essentially hold the array of PTEs and provide functions to get and add PTEs
- PageTableEntry class - each PTE - storing info about the page (frameNum, valid, reference, and dirty bits, as well as GET methods to obtain those values)
- class for each method - variables for the entire class, a constructor to set variable values, and a run method with helper methods
	- optAlgo class
	- clockAlgo class
	- nruAlgo class
	- fifoAlgo class	
*/

//import statements
import java.lang.*;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Arrays;

public class vmsim {
    
    public static void main(String[]args) {
        
        if(args.length < 5 || args.length == 6 || args.length > 7) {
            System.out.println("Error: invalid number of parameters supplied");
			return;
        }
        
        int numFrames=0;
        String pickedAlg="";
        int refresh = -1;
        String tracefile = "";
		int totalMemAcc=0, totalPgFaults=0, totalWritesToDisk=0;
        
        if(!args[0].equals("-n")) {
            System.out.println("Error: invalid first parameter supplied. Should be '-n'"); 
            return;
        }
        numFrames = Integer.parseInt(args[1]);
        
        if(!args[2].equals("-a")) {
            System.out.println("Error: invalid second parameter supplied. Should be '-a'"); 
            return;
        }
        pickedAlg = args[3];
        
        if(pickedAlg.equals("nru")) {
			if(!args[4].equals("-r")) {
				System.out.println("Error: '-r' not supplied.");
				return;
			}
			else {
				if(args.length < 6) {
					System.out.println("Error: refresh parameter value not supplied. Expected after '-r'");
					return;
				}
				else {
					refresh = Integer.parseInt(args[5]);
				}
			}
		}
        tracefile = args[args.length-1];
        //open the tracefile and check to make sure they supplied a valid tracefile name
        
        //now that all of the parameters have been set...
        
        if(pickedAlg.equals("opt")) {
            OPT(tracefile, numFrames);
        }
        else if(pickedAlg.equals("clock")) {
            CLOCK(tracefile, numFrames);
        }
        else if(pickedAlg.equals("nru")) {
            NRU(tracefile, numFrames, refresh);
        }
		else if(pickedAlg.equals("fifo"))
		{
			FIFO(tracefile, numFrames);
		}
        else {
            System.out.println("Error: invalid algorithm name supplied. The only algorithms available are: opt, clock, nru, and rand");
            return;
        }
		
		
        //Keep these in each algorithm's implementation, not in main method
        /*System.out.println("Total memory accesses:  "+totalMemAcc+"");
        System.out.println("Total page faults:  "+totalPgFaults+"");
        System.out.println("Total writes to disk:   "+totalWritesToDisk+"");*/
        
    }
    
    public static void OPT(String tracefile, int frames) {
        //opt algorithm   
		optAlgo runOPT = new optAlgo(tracefile, frames);
		runOPT.run();
    }
    public static void CLOCK(String tracefile, int frames) {
        //clock algorithm   
		clockAlgo runCLOCK = new clockAlgo(tracefile, frames);
		runCLOCK.run();
    }
    public static void NRU(String tracefile, int frames, int refresh) {
        //nru algorithm  
		nruAlgo runNRU = new nruAlgo(tracefile, frames, refresh);
		runNRU.run();
    }
	public static void FIFO(String tracefile, int frames) {
        //fifo algorithm   
		fifoAlgo runFIFO = new fifoAlgo(tracefile, frames); 
		runFIFO.run();
    }
}

class PageTable {
	private int pageSize = 4096;	//2^12
	private int numPages = 1048576;	//2^20
	private PageTableEntry[] table;
	
	public PageTable() {
		this.table = new PageTableEntry[numPages];
	}
	public PageTableEntry getPage(int pageNum) {
		return this.table[pageNum];
	}
	public boolean addPage(int pageNum, int valid, int reference, int dirty, int frameNum) {
		if(this.table[pageNum]==null) {
			PageTableEntry temp = new PageTableEntry(frameNum, valid, reference, dirty);
			this.table[pageNum] = temp;
			return true;
		}
		return false;
	}
}

class PageTableEntry {
	private int frameNum;
	private int v;
	private int r;
	private int d;
	
	public PageTableEntry(int frameNum, int valid, int reference, int dirty) {
		this.frameNum = frameNum;
		this.v = valid;
		this.r = reference;
		this.d = dirty;
	}
	public int getFrameNum() {
		return this.frameNum;
	}
	public void setFrameNum(int f) {
		this.frameNum=f;
	}
	public int getV() {
		return this.v;
	}
	public void setV(int v) {
		this.v=v;
	}
	public int getR() {
		return this.r;
	}
	public void setR(int r) {
		this.r=r;
	}
	public int getD() {
		return this.d;
	}
	public void setD(int d) {
		this.d=d;
	}
}

class optAlgo {
	private String filename;
	private int numFrames;
	private int totalMemAcc, totalPgFaults, totalWritesToDisk;
	private int[] frameTable;
	private PageTable table;
	private Random rand;
	private Hashtable<Integer, LinkedList<Integer>> hashtable;
	
	public optAlgo(String file, int frames) {
		this.filename = file;
		this.numFrames = frames;
		this.totalMemAcc = 0;
		this.totalPgFaults = 0;
		this.totalWritesToDisk = 0;
		this.frameTable = new int[numFrames];
		this.table = new PageTable();
		for(int i=0; i<numFrames; i++) {
			this.frameTable[i]=-1;
		}
		rand = new Random();
		hashtable = new Hashtable<Integer, LinkedList<Integer>>(1048576);
		for(int i=0;i<1048576;i++) {
			LinkedList<Integer> list = new LinkedList<Integer>();
			hashtable.put(i, list);
		}
	}
	public boolean run() {
		String temp="";
		int pageNum=0, dirty=0, frameNum=0;
		int indexCtr=0;
		try {
		BufferedReader bRead = new BufferedReader(new FileReader(new File (filename)));
		BufferedReader bRead2 = new BufferedReader(new FileReader(new File (filename)));
		//first loop through tracefile to populate hashtable<pageNum, LList<int>>
		while( (temp = bRead2.readLine()) != null ) {
			pageNum = Integer.parseInt(temp.substring(0, 5), 16);
			hashtable.get(pageNum).add(indexCtr);
			indexCtr++;
		}
		
		while( (temp = bRead.readLine()) != null ) 
		{
							//System.out.println("CURRENT\t\t READLINE: "+temp);
			pageNum = Integer.parseInt(temp.substring(0, 5), 16);
							//System.out.println("Page number: "+pageNum+"");
			PageTableEntry pte = table.getPage(pageNum);
							//System.out.println("got page");
			if(pte==null) {		//not currently in PageTable
				//initialize and add to table
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.addPage(pageNum, 1, 1, dirty, frameNum);	//since not in PageTable, need to add the new page 
			}
			else {	//simply change the PTE values - reference, valid, and dirty bits
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.getPage(pageNum).setR(1);
				table.getPage(pageNum).setD(dirty);
				table.getPage(pageNum).setV(1);
				
			}
			if(inFrameTable(pageNum)) {		//current reference is already in frameTable, no page fault!
				System.out.println("Hit");
				hashtable.get(pageNum).removeFirst();
			}
			else {		//page fault, need to add to frameTable
				int pageToBeEvicted=-1;
				totalPgFaults++;
				if(frameNum < numFrames) {	//Space available in frameTable
					frameTable[frameNum] = pageNum;
					frameNum++;
					hashtable.get(pageNum).removeFirst();
					System.out.println("Page Fault - No Eviction");
				}
				else {	//frameTable is full
					//select page to be evicted
					hashtable.get(pageNum).removeFirst();
					pageToBeEvicted = optSelectEvictPage();
					
					/*if(hashtable.get(frameTable[pageToBeEvicted]).size() > 0) {
						hashtable.get(frameTable[pageToBeEvicted]).removeFirst();
					}*/
					
					//remember that pageToBeEvicted is the index of pageNum in the frameTable that will be evicted, not the pageNum
					table.getPage(frameTable[pageToBeEvicted]).setV(0);		//taking it out of frameTable - so it is invalid in pageTable now
					table.getPage(frameTable[pageToBeEvicted]).setR(0);		//this means it is not referenced anymore either
					
					if(table.getPage(frameTable[pageToBeEvicted]).getD()==1) {
						//need to write to disk
						totalWritesToDisk++;
						System.out.println("Page Fault - Evict Dirty"); //dirty page written to disk
						table.getPage(frameTable[pageToBeEvicted]).setD(0);
					}
					else {
						System.out.println("Page Fault - Evict Clean");
					}
					
					frameTable[pageToBeEvicted] = pageNum;		//replace the evicted page with the current reference in frameTable
						//already set valid bit earlier when dealing with the pageTable
					
					
				}
			}
			totalMemAcc++;	//each memory reference, regardless of result, is a memory access
			
			//check if in frameTable
				//if it is, Hit
				//else		//totalPgFaults++
					//if space available in frameTable, Page Fault - No Eviction
					//else, select a page to be evicted
						//this is where the specific page replacement algorithm comes in
						//if the dirty bit for the pageToBeEvicted == 0, Page Fault - Evict Clean
						//else, Page Fault - Evict Dirty	//totalWritesToDisk++
			
			//totalMemAcc++
		}
			//System.out.println(hashtable.toString());
			System.out.println("Algorithm:	OPT");
			System.out.println("Number of frames:	"+numFrames+"");
			System.out.println("Total memory accesses:  "+totalMemAcc+"");
			System.out.println("Total page faults:  "+totalPgFaults+"");
			System.out.println("Total writes to disk:   "+totalWritesToDisk+"");
		} catch(IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public boolean inFrameTable(int pageNum) {		//just to check whether the page is in the frameTable
		for(int i=0; i<frameTable.length; i++) {			
			if(frameTable[i]==pageNum) return true;
		}
		return false;
	}
	
	public int optSelectEvictPage() {
		int maxIndex = 0;
		int maxIndexPageNum = 0;
		for(int i=0;i<numFrames;i++) {	//to select a page to be evicted, we need to see which valid frame has the largest next tracefile index and select that OR select a page that has an empty linkedlist
			if(hashtable.get(frameTable[i]).peekFirst() != null) {
				if(hashtable.get(frameTable[i]).peekFirst() > maxIndex) {	//if it is greater
					maxIndex = hashtable.get(frameTable[i]).peekFirst();		//set maxIndex value to the first value in the page's LList
					maxIndexPageNum = i;										//save frameTable index of the page
				}
						
			}
			else {	//this page has no more memory references, so return the index of it in frameTable
				return i;
			}
		}
		return maxIndexPageNum;
	}
}

class clockAlgo {
	private String filename;
	private int numFrames;
	private int totalMemAcc, totalPgFaults, totalWritesToDisk;
	private int[] frameTable;
	private PageTable table;
	
	public clockAlgo(String file, int frames) {
		this.filename = file;
		this.numFrames = frames;
		this.totalMemAcc = 0;
		this.totalPgFaults = 0;
		this.totalWritesToDisk = 0;
		this.frameTable = new int[numFrames];
		this.table = new PageTable();
		for(int i=0; i<numFrames; i++) {
			this.frameTable[i]=-1;
		}
	}
	public boolean run() {
		String temp="";
		int pageNum=0, dirty=0, frameNum=0;
		int pointer=0;		//used for clock algorithm
		try {
		BufferedReader bRead = new BufferedReader(new FileReader(new File (filename)));
		while( (temp = bRead.readLine()) != null ) {
							//System.out.println("CURRENT\t\t READLINE: "+temp);
			pageNum = Integer.parseInt(temp.substring(0, 5), 16);
							//System.out.println("Page number: "+pageNum+"");
			PageTableEntry pte = table.getPage(pageNum);
							//System.out.println("got page");
			if(pte==null) {		//not currently in PageTable
				//initialize and add to table
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.addPage(pageNum, 1, 1, dirty, frameNum);	//since not in PageTable, need to add the new page 
			}
			else {	//simply change the PTE values - reference, valid, and dirty bits
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.getPage(pageNum).setR(1);
				table.getPage(pageNum).setD(dirty);
				table.getPage(pageNum).setV(1);
				
			}
			if(inFrameTable(pageNum)) {		//current reference is already in frameTable, no page fault!
				System.out.println("Hit");
			}
			else {		//page fault, need to add to frameTable
				totalPgFaults++;
				if(frameNum < numFrames) {	//Space available in frameTable
					frameTable[frameNum] = pageNum;
					frameNum++;
					System.out.println("Page Fault - No Eviction");
				}
				else {	//frameTable is full
					//select page to be evicted
					while(true) {
						if(table.getPage(frameTable[pointer]).getR()==1) {	//still referenced in current cycle
							table.getPage(frameTable[pointer]).setR(0);		//reset R bit to 0
							pointer++;										//increment pointer and move on
							if(pointer==numFrames) pointer=0;				//reset to 0 if it is currently indicating out of bounds
						}
						else {		//found the page to be evicted - ref bit is equal to 0
							table.getPage(frameTable[pointer]).setV(0);		//taking it out of frameTable - so it is invalid in pageTable now
							table.getPage(frameTable[pointer]).setR(0);		//this means it is not referenced anymore either
							if(table.getPage(frameTable[pointer]).getD()==1) {
								//need to write to disk
								totalWritesToDisk++;
								System.out.println("Page Fault - Evict Dirty");
								table.getPage(frameTable[pointer]).setD(0);
							}
							else {
								System.out.println("Page Fault - Evict Clean");
							}
							
							frameTable[pointer] = pageNum;		//replace the evicted page with the current reference in frameTable
								//already set valid bit earlier when dealing with the pageTable
					
							
							
							pointer++;
							if(pointer==numFrames) pointer=0;				//reset to 0 if it is currently indicating out of bounds
							break;
						}
					}
					//remember that pageToBeEvicted is the index in the frameTable of pageNum to be evicted, not the pageNum
					
				}
			}
			totalMemAcc++;	//each memory reference, regardless of result, is a memory access
			
			//check if in frameTable
				//if it is, Hit
				//else		//totalPgFaults++
					//if space available in frameTable, Page Fault - No Eviction
					//else, select a page to be evicted
						//this is where the specific page replacement algorithm comes in
						//if the dirty bit for the pageToBeEvicted == 0, Page Fault - Evict Clean
						//else, Page Fault - Evict Dirty	//totalWritesToDisk++
			
			//totalMemAcc++
		}
			System.out.println("Algorithm:	Clock");
			System.out.println("Number of frames:	"+numFrames+"");
			System.out.println("Total memory accesses:  "+totalMemAcc+"");
			System.out.println("Total page faults:  "+totalPgFaults+"");
			System.out.println("Total writes to disk:   "+totalWritesToDisk+"");
		} catch(IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public boolean inFrameTable(int pageNum) {		//just to check whether the page is in the frameTable
		for(int i=0; i<frameTable.length; i++) {			
			if(frameTable[i]==pageNum) return true;
		}
		return false;
	}
}

class nruAlgo {
	private String filename;
	private int numFrames;
	private int totalMemAcc, totalPgFaults, totalWritesToDisk;
	private int[] frameTable;
	private PageTable table;
	private Random rand;
	private int refresh;
	private int rCtr;
	private ArrayList<Integer> class0;
	private ArrayList<Integer> class1;
	private ArrayList<Integer> class2;
	private ArrayList<Integer> class3;
	
	public nruAlgo(String file, int frames, int refresh) {
		this.filename = file;
		this.numFrames = frames;
		this.totalMemAcc = 0;
		this.totalPgFaults = 0;
		this.totalWritesToDisk = 0;
		this.frameTable = new int[numFrames];
		this.table = new PageTable();
		for(int i=0; i<numFrames; i++) {
			this.frameTable[i]=-1;
		}
		this.rand = new Random();
		this.refresh = refresh;
		rCtr = 0;
		this.class0 = new ArrayList<Integer>();
		this.class1 = new ArrayList<Integer>();
		this.class2 = new ArrayList<Integer>();
		this.class3 = new ArrayList<Integer>();
	}
	public boolean run() {
		String temp="";
		int pageNum=0, dirty=0, frameNum=0;
		try {
		BufferedReader bRead = new BufferedReader(new FileReader(new File (filename)));
		while( (temp = bRead.readLine()) != null ) {
							//System.out.println("CURRENT\t\t READLINE: "+temp);
			pageNum = Integer.parseInt(temp.substring(0, 5), 16);
							//System.out.println("Page number: "+pageNum+"");
			PageTableEntry pte = table.getPage(pageNum);
							//System.out.println("got page");
			if(pte==null) {		//not currently in PageTable
				//initialize and add to table
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.addPage(pageNum, 1, 1, dirty, frameNum);	//since not in PageTable, need to add the new page 
			}
			else {	//simply change the PTE values - reference, valid, and dirty bits
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.getPage(pageNum).setR(1);
				table.getPage(pageNum).setD(dirty);
				table.getPage(pageNum).setV(1);
				
			}
			if(inFrameTable(pageNum)) {		//current reference is already in frameTable, no page fault!
				System.out.println("Hit");
			}
			else {		//page fault, need to add to frameTable
				totalPgFaults++;
				if(frameNum < numFrames) {	//Space available in frameTable
					frameTable[frameNum] = pageNum;
					frameNum++;
					System.out.println("Page Fault - No Eviction");
					
					if(table.getPage(pageNum).getR()==0) {
						if(table.getPage(pageNum).getD()==0) {
							//class 0
							class0.add(frameNum-1);		//already incremented frameNum
						}
						else {
							//class 1
							class1.add(frameNum-1);		//^
						}
					}
					else {
						if(table.getPage(pageNum).getD()==0) {
							//class 2
							class2.add(frameNum-1);		//^
						}
						else {
							//class 3
							class3.add(frameNum-1);		//^
							
						}
					}
				}
				else {	//frameTable is full
					//select page to be evicted
					
					int pageToBeEvicted = -1;
					int classIndex = -1;
					//begin with class0 til class3. empty means going up to the next class, and there will always be at least one in the class4 list
					if(class0.size() < 1) {
						if(class1.size() < 1) {
							if(class2.size() < 1) {
								//take from class 3
								classIndex = rand.nextInt(class3.size());
								pageToBeEvicted = class3.get(classIndex);
								class3.remove(classIndex);
							}
							else {
								//take from class 2
								classIndex = rand.nextInt(class2.size());
								pageToBeEvicted = class2.get(classIndex);
								class2.remove(classIndex);
							}
						}
						else {
							//take from class 1
							classIndex = rand.nextInt(class1.size());
							pageToBeEvicted = class1.get(classIndex);
							class1.remove(classIndex);
						}
					}
					else {
						//take from class 0
						classIndex = rand.nextInt(class0.size());
						pageToBeEvicted = class0.get(classIndex);
						class0.remove(classIndex);	
					}
					//pageToBeEvicted is the index of pageNum found in frameTable that will be evicted
					table.getPage(frameTable[pageToBeEvicted]).setV(0);		//taking it out of frameTable - so it is invalid in pageTable now
					table.getPage(frameTable[pageToBeEvicted]).setR(0);		//this means it is not referenced anymore either
					
					if(table.getPage(frameTable[pageToBeEvicted]).getD()==1) {
						//need to write to disk
						totalWritesToDisk++;
						System.out.println("Page Fault - Evict Dirty");
						table.getPage(frameTable[pageToBeEvicted]).setD(0);
					}
					else {
						System.out.println("Page Fault - Evict Clean");
					}
					frameTable[pageToBeEvicted] = pageNum;		//replace the evicted page with the current reference in frameTable
						//already set valid bit earlier when dealing with the pageTable
					
					
					
					if(table.getPage(pageNum).getD()==0) {	//at this point, the current memory reference has replaced the evicted page in the frameTable
						//current memory reference is clean - class 2
						class2.add(pageToBeEvicted);
					}
					else {
						//current memory reference is dirty - class 3
						class3.add(pageToBeEvicted);
					}
				}
			}
			totalMemAcc++;	//each memory reference, regardless of result, is a memory access
			rCtr++;			//incrementing the refresh counter after each memory reference
			if(rCtr == refresh) {	//time for refresh
				for(int i=0;i<class0.size();i++) {
					table.getPage(frameTable[class0.get(i)]).setR(0);
				}
				class0.clear();
				for(int i=0;i<class1.size();i++) {
					table.getPage(frameTable[class1.get(i)]).setR(0);
				}
				class1.clear();
				for(int i=0;i<class2.size();i++) {
					table.getPage(frameTable[class2.get(i)]).setR(0);
				}
				class2.clear();
				for(int i=0;i<class3.size();i++) {
					table.getPage(frameTable[class3.get(i)]).setR(0);
				}
				class3.clear();
				rCtr=0;
				classSetup(frameNum, rCtr);
			}
			
			//check if in frameTable
				//if it is, Hit
				//else		//totalPgFaults++
					//if space available in frameTable, Page Fault - No Eviction
					//else, select a page to be evicted
						//this is where the specific page replacement algorithm comes in
						//if the dirty bit for the pageToBeEvicted == 0, Page Fault - Evict Clean
						//else, Page Fault - Evict Dirty	//totalWritesToDisk++
			
			//totalMemAcc++
		}
			System.out.println("Algorithm:	NRU");
			System.out.println("Number of frames:	"+numFrames+"");
			System.out.println("Total memory accesses:  "+totalMemAcc+"");
			System.out.println("Total page faults:  "+totalPgFaults+"");
			System.out.println("Total writes to disk:   "+totalWritesToDisk+"");
		} catch(IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public boolean inFrameTable(int pageNum) {		//just to check whether the page is in the frameTable
		for(int i=0; i<frameTable.length; i++) {			
			if(frameTable[i]==pageNum) return true;
		}
		return false;
	}
	
	public boolean classSetup(int frameNum, int rCtr) {
		int max=0;
		if(frameNum >= numFrames) max=numFrames;
		else max=frameNum;
		if(rCtr==0) {
			for(int i=0; i<max; i++) {
				if(table.getPage(frameTable[i]).getR()==0) {
					if(table.getPage(frameTable[i]).getD()==0) {
						//class 0
						class0.add(i);
					}
					else {
						//class 1
						class1.add(i);
					}
				}
				else {
					if(table.getPage(frameTable[i]).getD()==0) {
						//class 2
						class2.add(i);
					}
					else {
						class3.add(i);
						//class 3
					}
				}
			}
		return true;
		}
		else return false;
	}
}



class fifoAlgo {
	
	private String filename;
	private int numFrames;
	private int totalMemAcc, totalPgFaults, totalWritesToDisk;
	private int[] frameTable;
	private PageTable table;
	private Random rand;
	
	public fifoAlgo(String file, int frames) {
		this.filename = file;
		this.numFrames = frames;
		this.totalMemAcc = 0;
		this.totalPgFaults = 0;
		this.totalWritesToDisk = 0;
		this.frameTable = new int[numFrames];
		this.table = new PageTable();
		for(int i=0; i<numFrames; i++) {
			this.frameTable[i]=-1;
		}
		
		this.rand = new Random();
	}
	public boolean run() {
		String temp="";
		int pageNum=0, dirty=0, frameNum=0;
		try 
		{
		BufferedReader bRead = new BufferedReader(new FileReader(new File (filename)));
		while( (temp = bRead.readLine()) != null ) 
		{
							//System.out.println("CURRENT\t\t READLINE: "+temp);
			pageNum = Integer.parseInt(temp.substring(0, 5), 16);
							//System.out.println("Page number: "+pageNum+"");
			PageTableEntry pte = table.getPage(pageNum);
							//System.out.println("got page");
			if(pte==null) {		//not currently in PageTable
				//initialize and add to table
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.addPage(pageNum, 1, 1, dirty, frameNum);	//since not in PageTable, need to add the new page 
			}
			else {	//simply change the PTE values - reference, valid, and dirty bits
				if(temp.substring(temp.length()-1).equals("W")) {
					dirty = 1;
				}
				else {
					dirty = 0;
				}
				table.getPage(pageNum).setR(1);
				table.getPage(pageNum).setD(dirty);
				table.getPage(pageNum).setV(1);
				
			}
			if(inFrameTable(pageNum)) {		//current reference is already in frameTable, no page fault!
				System.out.println("Hit");
			}
			else {		//page fault, need to add to frameTable
				totalPgFaults++;
				if(frameNum < numFrames) {	//Space available in frameTable
					frameTable[frameNum] = pageNum;
					frameNum++;
					System.out.println("Page Fault - No Eviction");
				}
				else {	//frameTable is full
					//select page to be evicted
					int pageToBeEvicted = rand.nextInt(numFrames);	//generate a number between 0 and numFrames, inclusively and exclusively (i.e. 4 frames means 0/1/2/3)
					//remember that pageToBeEvicted is the index of pageNum in the frameTable that will be evicted, not the pageNum
					table.getPage(frameTable[pageToBeEvicted]).setV(0);		//taking it out of frameTable - so it is invalid in pageTable now
					table.getPage(frameTable[pageToBeEvicted]).setR(0);		//this means it is not referenced anymore either
					//int pageToBeEvicted = class0.nextInt(numFrames);
					if(table.getPage(frameTable[pageToBeEvicted]).getD()==1) {
						//need to write to disk
						totalWritesToDisk++;
						System.out.println("Page Fault - Evict Dirty");
						table.getPage(frameTable[pageToBeEvicted]).setD(0);
					}
					else {
						System.out.println("Page Fault - Evict Clean");
					}
					
					frameTable[pageToBeEvicted] = pageNum;		//replace the evicted page with the current reference in frameTable
						//already set valid bit earlier when dealing with the pageTable
					
					
				}
			}
			totalMemAcc++;	//each memory reference, regardless of result, is a memory access
			
			//check if in frameTable
				//if it is, Hit
				//else		//totalPgFaults++
					//if space available in frameTable, Page Fault - No Eviction
					//else, select a page to be evicted
						//this is where the specific page replacement algorithm comes in
						//if the dirty bit for the pageToBeEvicted == 0, Page Fault - Evict Clean
						//else, Page Fault - Evict Dirty	//totalWritesToDisk++
			
			//totalMemAcc++
		}
			System.out.println("Algorithm:	FIFO");
			System.out.println("Number of frames:	"+numFrames+"");
			System.out.println("Total memory accesses:  "+totalMemAcc+"");
			System.out.println("Total page faults:  "+totalPgFaults+"");
			System.out.println("Total writes to disk:   "+totalWritesToDisk+"");
		} 
		catch(IOException e) 
		{
			e.printStackTrace();
		}
		return false;
	}
	public boolean inFrameTable(int pageNum) {		//just to check whether the page is in the frameTable
		for(int i=0; i<frameTable.length; i++) {			
			if(frameTable[i]==pageNum) return true;
		}
		return false;
	}
	
}//end of fifoAlgo class 