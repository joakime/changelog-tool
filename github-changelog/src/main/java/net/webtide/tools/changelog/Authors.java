package net.webtide.tools.changelog;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Authors extends ArrayList<Author>
{
    public static Authors load() throws IOException
    {
        URL url = Authors.class.getClassLoader().getResource("authors.json");
        try (InputStream in = url.openStream();
             InputStreamReader reader = new InputStreamReader(in, UTF_8))
        {
            Gson gson = new Gson();
            Authors authors = gson.fromJson(reader, Authors.class);
            return authors;
        }
    }

    private Map<String, Integer> emailMap = new HashMap<>();

    public Authors()
    {
//        add(new Author("Greg Wilkins").committer(true).email("gregw@webtide.com"));
//        add(new Author("Jan Bartel").committer(true).email("janb@webtide.com"));
//        add(new Author("Simone Bordet").committer(true).email("simone.bordet@gmail.com"));
//        add(new Author("Jesse McConnell").committer(true).email("jesse.mcconnell@gmail.com"));

//        add("gregw@webtide.com", "Greg Wilkins", true);
//        add("janb@webtide.com", "Jan Bartel", true);
//        add("simone.bordet@gmail.com", "Simone Bordet", true);
//        add("jesse.mcconnell@gmail.com", "Jesse McConnell", true);
        /*
        add("oliver.lamy@gmail.com", "Olivier Lamy", true);
        add("olamy@webtide.com", "Olivier Lamy", true);
        add("olamy@apache.org", "Olivier Lamy", true);
        add("ctwalker@gmail.com", "Chris Walker", true);
        add("lachlan@webtide.com", "Lachlan Roberts", true);
        add("lorban@bitronix.be", "Ludovic Orban", true);
         */
    }

    @Override
    public boolean add(Author author)
    {
        boolean ret = super.add(author);
        updateEmailMap();
        return ret;
    }

    public Author find(String email)
    {
        Integer idx = emailMap.get(email);
        if (idx == null)
            return null;
        return get(idx);
    }

    private void updateEmailMap()
    {
        emailMap.clear();
        int size = this.size();
        for (int i = 0; i < size; i++)
        {
            Author author = this.get(i);
            for (String email : author.emails())
            {
                emailMap.put(email, i);
            }
        }
    }

    public boolean isCommitter(String email)
    {
        Integer idx = emailMap.get(email);
        if (idx == null)
            return false;
        Author author = get(idx);
        if (author == null)
            return false;
        return author.committer();
    }
}
