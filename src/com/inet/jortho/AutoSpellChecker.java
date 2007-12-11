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
 *  Created on 05.11.2005
 */
package com.inet.jortho;

import java.util.Locale;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.Highlight;

/**
 * This class check a <code>JTextComponent</code> automaticly (in the background) for orthography. Spell error are
 * highligted with a red zigzag line.
 * 
 * @author Volker Berlin
 */
class AutoSpellChecker implements DocumentListener, LanguageChangeListener {
    private static final RedZigZagPainter painter = new RedZigZagPainter();

    private JTextComponent                jText;

    private Dictionary                    dictionary;

    private Locale                        locale;

    
    public AutoSpellChecker(JTextComponent text){
        this.jText = text;
        jText.getDocument().addDocumentListener( this );

        SpellChecker.addLanguageChangeLister( this );
        dictionary = SpellChecker.getCurrentDictionary();
        locale = SpellChecker.getCurrentLocale();
        checkAll();
    }




    /*====================================================================
     * 
     * Methods of interface DocumentListener
     * 
     *===================================================================*/

    /**
     * {@inheritDoc}
     */
    public void changedUpdate( DocumentEvent e ) {
        //Nothing
    }

    /**
     * {@inheritDoc}
     */
    public void insertUpdate( DocumentEvent e ) {
        checkCurrentElement();
    }

    /**
     * {@inheritDoc}
     */
    public void removeUpdate( DocumentEvent e ) {
        checkCurrentElement();
    }

    /**
     * Check the Elment on the current cursor position.
     */
    private void checkCurrentElement() {
        int i = jText.getSelectionStart();
        Document document = jText.getDocument();
        Element element;

        try {
            element = ((javax.swing.text.StyledDocument)document).getCharacterElement( i );
        } catch( java.lang.Exception exception ) {
            try {
                element = ((AbstractDocument)document).getParagraphElement( i );
            } catch( java.lang.Exception ex ) {
                return;
            }
        }
        checkElement( element );
    }

    /**
     * Check the spelling of the text of an element.
     * 
     * @param element
     *            the to checking Element
     */
    private void checkElement( javax.swing.text.Element element ) {
        try {
            int i = element.getStartOffset();
            int j = element.getEndOffset();
            Highlighter highlighter = jText.getHighlighter();
            Highlight[] highlights = highlighter.getHighlights();
            for( int k = highlights.length; --k >= 0; ) {
                Highlight highlight = highlights[k];
                if( highlight.getStartOffset() >= i && highlight.getEndOffset() <= j ) {
                    highlighter.removeHighlight( highlight );
                }
            }

            int l = ((AbstractDocument)jText.getDocument()).getLength();
            j = Math.min( j, l );
            if( i >= j )
                return;
            String phrase = jText.getText( i, j - i );

            Tokenizer tok = new Tokenizer( phrase, dictionary, locale );
            String word;
            while( (word = tok.nextInvalidWord()) != null ) {
                int wordOffset = i + tok.getWordOffset();
                highlighter.addHighlight( wordOffset, wordOffset + word.length(), painter );
            }
            /*BreakIterator sentences = BreakIterator.getSentenceInstance( locale );
            sentences.setText( phrase );
            for( int start = sentences.first(), end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next() ) {
                String sentence = phrase.substring( start, end );
                BreakIterator words = BreakIterator.getWordInstance( locale );
                words.setText( sentence );
                for( int s = words.first(), e = words.next(); e != BreakIterator.DONE; s = e, e = words.next() ) {
                    String word = sentence.substring( s, e ).trim();
                    if(word.length() > 0 && Character.isLetter( word.charAt( 0 ) ) && !dictionary.exist( word ) ) {
                        int wordOffset = i + start + s;
                        highlighter.addHighlight( wordOffset, wordOffset + word.length(), painter );
                        System.out.println(word);
                    }
                }
            }*/
        } catch( BadLocationException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Check the completly text. Because this can consume many times with large Documents that this will do in a thread
     * in the background step by step.
     */
    private void checkAll() {
        if( dictionary == null || jText == null ) {
            //the needed objects does not exists
            return;
        }
        Thread thread = new Thread( new Runnable() {
            public void run() {
                Document document = jText.getDocument();
                for( int i = 0; i < document.getLength(); ) {
                    try {
                        final Element element = ((AbstractDocument)document).getParagraphElement( i );
                        i = element.getEndOffset();
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                checkElement( element );
                            }

                        } );
                    } catch( java.lang.Exception ex ) {
                        return;
                    }
                }
            }
        }, "JOrtho checkall" );
        thread.setPriority( Thread.NORM_PRIORITY - 1 );
        thread.setDaemon( true );
        thread.start();
    }

    public void languageChanged( LanguageChangeEvent ev ) {
        dictionary = SpellChecker.getCurrentDictionary();
        locale = SpellChecker.getCurrentLocale();
        checkAll();
    }

}
