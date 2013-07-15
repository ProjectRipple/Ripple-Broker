// James West
// CS 6800
// Assignment 3 - WIS Twitter
$(function() 
    {
    
        var refreshTweetsBtn = $('#refreshtweets');
        var tweetInput = $('#tweettext');
        var TWEET_LIMIT_NUM = 140;
        var charCountdown = $('#tweetlengthcountdown');
        var tweetPostedMessage = $('#tweetsuccessmessage');
        // Assign click function to refresh tweets
        refreshTweetsBtn.click(function()
            {
                var recentTweets = $('#recenttweets');

                $.getJSON('../php/GetRecentTweets.php', {}, function(data)
                    {
                        if(!data.tweets)
                        {
                            // response does not have tweets, check for message
                            if(data.message)
                            {
                                recentTweets.text(data.message);
                            }
                            else
                            {
                                recentTweets.text("Unknown error occured while retrieving recent tweets");
                            }
                            return;
                        }
                        // clear old content
                        recentTweets.text("");
                        for (var i = 0; i < data.tweets.length; i++)
                        {
                            recentTweets.append(data.tweets[i].date + '\n');
                            recentTweets.append(data.tweets[i].author + ' : ' + data.tweets[i].text + '\n\n');
                        }
                    }
                )
            }
        )
        
        // Get tweets automatically upon load
        refreshTweetsBtn.click();
        
        
        
        // Assign click function to post tweets
        $('#posttweet').click(function()
            {
                var widInput = $('#wid');
                
                $.post('../php/PostTweet.php', { 'wid':widInput.val(), 'tweet':tweetInput.val()}, function(data)
                    {
                        
                        if(data.success)
                        {
                            // clear old tweet from screen
                            tweetInput.val('');
                            // Reset character countdown
                            tweetInput.keydown();
                            // auto refresh the recent tweets
                            refreshTweetsBtn.click();
                            // Show message that tweet was posted
                            tweetPostedMessage.show('slow');
                            // Set timeout to fade out the tweet posted message
                            setTimeout(function()
                            {
                                tweetPostedMessage.fadeOut();
                            }, 3400);
                        }
                        else
                        {
                            // alert user to problem posting tweet
                            alert("Error posting tweet. \nError Message: " + data.message);
                        }
                    }
                    , "json"
                )
            }
        )
        
        

        function limitTweetText()
        {
            if(tweetInput.val().length > TWEET_LIMIT_NUM)
            {
                tweetInput.val( tweetInput.val().substring(0, TWEET_LIMIT_NUM));      
            }
            else
            {
                charCountdown.val(TWEET_LIMIT_NUM - tweetInput.val().length);
            }
            
        }
        
        tweetInput.keydown(limitTweetText);
        tweetInput.keyup(limitTweetText);
    }
)