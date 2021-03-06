package search;

import java.io.*;
import java.util.*;

/**
 * This class encapsulates an occurrence of a keyword in a document. It stores the
 * document name, and the frequency of occurrence in that document. Occurrences are
 * associated with keywords in an index hash table.
 * 
 * @author Sesh Venugopal
 * 
 */
class Occurrence {
	/**
	 * Document in which a keyword occurs.
	 */
	String document;
	
	/**
	 * The frequency (number of times) the keyword occurs in the above document.
	 */
	int frequency;
	
	/**
	 * Initializes this occurrence with the given document,frequency pair.
	 * 
	 * @param doc Document name
	 * @param freq Frequency
	 */
	public Occurrence(String doc, int freq) 
	{
		document = doc;
		frequency = freq;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() 
	{
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of documents in
 * which it occurs, with frequency of occurrence in each document. Once the index is built,
 * the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine 
{	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in descending
	 * order of occurrence frequencies.
	 */
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	
	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String,String> noiseWords;
	
	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashMap<String,String>(100,2.0f);
	}
	
	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) throws FileNotFoundException 
	{
		// load noise words to hash table
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) 
		{
			String word = sc.next();
			noiseWords.put(word,word);
		}
		
		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) 
		{
			String docFile = sc.next();
			HashMap<String,Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}
	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of keyword occurrences
	 * in the document. Uses the getKeyWord method to separate keywords from other words.
	 * 
	 * @param docFile Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated with an Occurrence object
	 * @throws FileNotFoundException If the document file is not found on disk
	 */
	public HashMap<String,Occurrence> loadKeyWords(String docFile) 
	throws FileNotFoundException 
	{
		if (docFile == null)
		{
			throw new FileNotFoundException();
		}
		
		HashMap<String,Occurrence> keywords = new HashMap<String,Occurrence>();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(new File(docFile));
		
		while (scan.hasNext()) //while the document has words to scan
		{
			String keyword = getKeyWord(scan.next());
			if (keyword != null) //if the word is a keyword
			{
				//add it to the list if it's not already in it
				if (keywords.get(keyword) == null)
				{
					keywords.put(keyword, new Occurrence(docFile, 1));
				}
				
				//If it's already in the list, increase its frequency
				else
				{
					keywords.get(keyword).frequency++;
				}
			}
		}

		return keywords;
	}
	
	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table. 
	 * This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws Keywords hash table for a document
	 */
	/*
	 * keywordsIndex contains all the keywords, and at each keyword, an ArrayList of occurrences
	 * of each word (each index in the ArrayList represents a different document)
	 */
	public void mergeKeyWords(HashMap<String,Occurrence> kws) 
	{
		for (String key : kws.keySet())
		{
			//adds a new ArrayList if it doesn't exist in keywordsIndex
			if (!keywordsIndex.containsKey(key))
			{
				ArrayList<Occurrence> occs = new ArrayList<Occurrence>();
				occs.add(kws.get(key));
				keywordsIndex.put(key, occs);
				//System.out.println("Merge");
			}
			
			//adds another occurrence to the already existing ArrayList then sorting it
			else
			{
				keywordsIndex.get(key).add(kws.get(key));
				insertLastOccurrence(keywordsIndex.get(key));
				keywordsIndex.put(key, keywordsIndex.get(key));
				//System.out.println("Merge");
			}
		}
	}
	
	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped of any
	 * TRAILING punctuation, consists only of alphabetic letters, and is not
	 * a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) 
	{
		word = word.toLowerCase(); //convert to lower-case
		if (word.length() == 1)
		{
			return null;
		}
		
		//Gets rid of trailing punctuation
		while (!Character.isLetter(word.charAt(word.length() - 1)))
		{
			char c = word.charAt(word.length() - 1);
			if (c == '.' || c == ',' || c == '?' || c == ':' || c == ';' || c == '!')
			{
				word = word.substring(0, word.length() - 1);
			}
			else
			{
				return null;
			}
		}
		
		//Checks for non-letters within the word
		for (int i = 0; i < word.length(); i++)
		{
			if (Character.isLetter(word.charAt(i)) == false)
			{
				return null;
			}
		}
		
		//checks if the word is one of the noise words
		if (noiseWords.containsKey(word))
		{
			return null;
		}
		
		return word;
	}
	
	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * same list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion of the last element
	 * (the one at index n-1) is done by first finding the correct spot using binary search, 
	 * then inserting at that spot.
	 * 
	 * @param occs List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the binary search process,
	 *         null if the size of the input list is 1. This returned array list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) 
	{
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		int n = occs.size();
		int low = 0, high = n - 2, mid;
		int lastFreq = occs.get(occs.size() - 1).frequency;
		
		//Binary search for the index to insert
		while (low < high)
		{
			mid = (low + high)/2;
			result.add(mid);
			if (occs.get(mid).frequency == occs.get(occs.size() - 1).frequency)
			{
				low = mid;
				high = mid;
				break;
			}
			else if (occs.get(mid).frequency > occs.get(occs.size() - 1).frequency)
			{
				low = mid + 1;
			}
			else
			{
				high = mid;
			}
		}
		
		//inserts occurrence into correct location		
		if (lastFreq > occs.get(low).frequency)
		{
			occs.add(low, occs.remove(occs.size() - 1));
		}
		else
		{
			occs.add(low + 1, occs.remove(occs.size() - 1));
		}
		
		return result;
	}
	
	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of occurrence frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will appear before doc2 in the result. 
	 * The result set is limited to 5 entries. If there are no matching documents, the result is null.
	 * 
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents. If there are no matching documents,
	 *         the result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) 
	{
		ArrayList<String> documents = new ArrayList<String>();
		ArrayList<Occurrence> occs1 = null;
		ArrayList<Occurrence> occs2 = null;
		
		if (keywordsIndex.get(kw1) != null)
		{
			occs1 = keywordsIndex.get(kw1);
		}
		
		if (keywordsIndex.get(kw2) != null)
		{
			occs2 = keywordsIndex.get(kw2);
		}
		
		if (occs1 == null && occs2 == null)
		{
			return documents;
		}
		
		//if either of the lists is null
		else if (occs1 != null && occs2 == null)
		{
			int index = 0;
			while (documents.size() < 5 && index < occs1.size())
			{
				if (!documents.contains(occs1.get(index)))
				{
					documents.add(occs1.get(index).document);
				}
				index++;
			}
		}
		else if (occs2 != null && occs1 == null)
		{
			int index = 0;
			while (documents.size() < 5 && index < occs2.size())
			{
				if (!documents.contains(occs2.get(index)))
				{
					documents.add(occs2.get(index).document);
				}
				index++;
			}
		}
		
		//when both have stuff in it
		else
		{
			int index1 = 0, index2 = 0;
			
			//traverse through both Occurrence ArrayLists of the 2 keywords while you can add to documents
			while (index1 < occs1.size() && index2 < occs2.size() && documents.size() < 5)
			{
				//if the frequency in list 1 is greater, then add it to the list and increment the index
				if (occs1.get(index1).frequency > occs2.get(index2).frequency)
				{
					if (!documents.contains(occs1.get(index1).document))
					{
						documents.add(occs1.get(index1).document);
					}
					index1++;
				}
				
				//do the same if the frequency in list 2 is greater
				else if (occs1.get(index1).frequency < occs2.get(index2).frequency)
				{
					if (!documents.contains(occs2.get(index2).document))
					{
						documents.add(occs2.get(index2).document);
					}
					index2++;
				}
			
				//add both, the first keyword frequency coming in first (make sure documents has room)
				else
				{
					if (!documents.contains(occs1.get(index1).document))
					{
						documents.add(occs1.get(index1).document);
					}
					if (documents.size() < 5 && !documents.contains(occs2.get(index2).document))
					{
						documents.add(occs2.get(index2).document);
					}
					index1++;
					index2++;
				}
			}
		
			//if the first list runs out prematurely, fill up remaining slots with the second list and vice-versa
			if (index1 == occs1.size())
			{
				while (index2 < occs2.size() && documents.size() < 5)
				{
					if (!documents.contains(occs2.get(index2).document))
					{
						documents.add(occs2.get(index2).document);
					}
					index2++;
				}
			}
			if (index2 == occs2.size())
			{
				while (index1 < occs1.size() && documents.size() < 5)
				{
					if (!documents.contains(occs1.get(index1).document))
					{
						documents.add(occs1.get(index1).document);
					}
					index1++;
				}
			}
		}
		
		return documents;
	}
}
