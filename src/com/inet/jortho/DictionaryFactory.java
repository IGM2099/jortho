/*
 *  JOrtho
 *
 *  Copyright (C) 2005-2007 by i-net software
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version. 
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *  
 *  Created on 15.06.2007
 */
package com.inet.jortho;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

/** 
 * @author Volker Berlin
 */
public class DictionaryFactory {

    private final Node root = new Node();
    private char[] tree;
    private int size;
    
    public DictionaryFactory(){
        
    }
    
    
    /**
     * Load the directory from a compressed list of words with UTF8 encoding.
     * The words must be delimmited with newlines.
     * @param filename the name of the file.
     * @throws IOException if an I/O error occurs.
     */
    public void loadWordList( URL filename ) throws IOException {
        InputStream input = filename.openStream();
        input = new InflaterInputStream( input );
        input = new BufferedInputStream( input );

        loadPlainWordList( input, "UTF8" );
    }
    
    /**
     * Load the directory from plain a list of words.
     * The words must be delimmited with newlines.
     * @param stream the InputStream with 
     * @throws IOException
     */
    public void loadPlainWordList(InputStream stream, String charsetName)  throws IOException{
        BufferedReader input = new BufferedReader(new InputStreamReader(stream, charsetName ));
        String word  = input.readLine();
        while(word != null){
            if(word.length() > 1){
                add( word );
            }
            word  = input.readLine();
        }
    }
    
    
    /**
     * Add a word to the tree. If it already exist then it has no effect. 
     * @param word the new word.
     */
    public void add(String word){
        Node node = root;
        for(int i=0; i<word.length(); i++){
            char c = word.charAt(i);
            NodeEntry entry = node.searchCharOrAdd( c );
            if(i == word.length()-1){
                entry.isWord = true;
                return;
            }
            Node nextNode = entry.nextNode;
            if(nextNode == null){
                node = entry.createNewNode();
            }else{
                node = nextNode;
            }
        }
    }

    
    public Dictionary create(){
        tree = new char[10000];
        
        root.save( this );
        
        //shrink the array
        char[] temp = new char[size];
        System.arraycopy( tree, 0, temp, 0, size );
        tree = temp;
        
        return new Dictionary(tree);
    }
    
    /**
     * Check the size of the array and resize it if needed.
     * @param newSize the requied size
     */
    final void checkSize(int newSize){
        if(newSize > tree.length){
            char[] puffer = new char[Math.max(newSize, 2*tree.length)];
            System.arraycopy(tree, 0, puffer, 0, size);
            tree = puffer;
        }
    }
    
    private final static class Node extends ArrayList{

        Node(){
            super(1);
        }
                
        NodeEntry searchCharOrAdd( char c ) {
            for(int i=0; i<size(); i++){
                NodeEntry entry = (NodeEntry)get( i );
                if(entry.c < c){
                    continue;
                }
                if(entry.c == c){
                    return entry;
                }
                entry = new NodeEntry(c);
                add( i, entry );
                trimToSize(); //reduce the memory consume, there is a very large count of this Nodes.
                return entry;
            }
            NodeEntry entry = new NodeEntry(c);
            add( entry );
            trimToSize(); //reduce the memory consume, there is a very large count of this Nodes.
            return entry;
        }
        
        int save(DictionaryFactory factory){
            int idx;
            int start = idx = factory.size;
            //reserve the needed memory
            int newSize = factory.size + size() * 3 + 1;
            factory.checkSize( newSize );
            factory.size = newSize;
            
            for(int i=0; i<size(); i++){
                NodeEntry entry = (NodeEntry)get( i );
                factory.tree[idx++] = entry.c;
                Node nextNode = entry.nextNode;
                int offset = 0;
                if(nextNode != null){
                    offset = nextNode.save(factory);
                }
                if(entry.isWord){
                    offset |= 0x80000000;
                }
                factory.tree[idx++] = (char)(offset >> 16);
                factory.tree[idx++] = (char)(offset);
            }
            factory.tree[idx] = 0xFFFF;
            return start;
        }
    }
    
    private final static class NodeEntry{
        final char c;
        Node nextNode;
        boolean isWord;
        
        NodeEntry(char c){
            this.c = c;
        }
        
        /**
         * Create a new Node and set it as nextNode
         * @return the nextNode
         */
        Node createNewNode() {
            return nextNode = new Node();
        }
    }
    
}
