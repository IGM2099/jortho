/*
 *  JOrtho
 *
 *  Copyright (C) 2005-2008 by i-net software
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
 *  Created on 24.12.2007
 */
package com.inet.jortho;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.*;



/**
 * Implements edit dialog for the user dictionary.
 * @author Volker Berlin
 */
class DictionaryEditDialog extends JDialog implements ActionListener{
    
    private final JList list;
    private final JButton delete;
    private boolean isModify;

    DictionaryEditDialog( JDialog parent ){
        super( parent, Utils.getResource("userDictionary"), true );
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
        Container content = getContentPane();
        content.setLayout( new GridBagLayout() );
        DefaultListModel data = new DefaultListModel();
        loadWordList( data );
        list = new JList( data );
        content.add( new JScrollPane(list), new GridBagConstraints( 1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH ,GridBagConstraints.BOTH, new Insets( 8,8,8,8 ), 0, 0) );
        
        delete = new JButton( Utils.getResource("delete") );
        content.add( delete, new GridBagConstraints( 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH ,GridBagConstraints.BOTH, new Insets( 0,8,8,8 ), 0, 0) );
        delete.addActionListener( this );
        
        //ESCAPE Taste
        getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0, false ), "ESCAPE" );
        getRootPane().getActionMap().put( "ESCAPE", new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                dispose();
            }
        } );

        pack();
        setLocationRelativeTo( parent );
    }
    
    /**
     * Load all words from the user dictionary if available
     * @param data
     */
    private void loadWordList( DefaultListModel data ){
        try{
            UserDictionaryProvider provider = SpellChecker.getUserDictionaryProvider();
            if( provider != null ) {
                String userWords = provider.getUserWords( SpellChecker.getCurrentLocale() );
                if( userWords != null ) {
                    BufferedReader input = new BufferedReader( new StringReader( userWords ) );
                    ArrayList<String> wordList = new ArrayList<String>();
                    String word = input.readLine();
                    while( word != null ) {
                        if( word.length() > 1 ) {
                            wordList.add( word );
                        }
                        word = input.readLine();
                    }
                    // Liste alphabetical sorting with the user language
                    Collections.sort( wordList, Collator.getInstance() );
                    for(String str : wordList){
                        data.addElement( str );
                    }
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }    
    }

    public void actionPerformed(ActionEvent e){
        int idx = list.getSelectedIndex();
        if( idx >= 0 ){
            ((DefaultListModel)list.getModel()).remove( list.getSelectedIndex() );
            isModify = true;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose(){
        super.dispose();
        if( isModify ){
            UserDictionaryProvider provider = SpellChecker.getUserDictionaryProvider();
            if( provider != null ) {
                ListModel model = list.getModel();
                StringBuilder builder = new StringBuilder();
                for( int i=0; i<model.getSize(); i++){
                    if( builder.length() != 0 ){
                        builder.append( '\n' );
                    }
                    builder.append( model.getElementAt(i) );
                }
                provider.setUserWords( builder.toString() );
            }
        }
    }
}
