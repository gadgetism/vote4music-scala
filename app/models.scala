package models

import java.util.Date
import javax.persistence._
import play.db.jpa.Model
import play.db.jpa.QueryOn
import play.db.jpa.JPA
import play.data.validation.Required
import javax.persistence.{EnumType, TemporalType, CascadeType}
import java.text.SimpleDateFormat


@Entity
class Album(
             @Required var name: String,
             @ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.MERGE))
             var artist: Artist,
             @Temporal(TemporalType.DATE) @Required var releaseDate: Date,
             var genre: String,
             var nbVotes: Long = 0L,
             var hasCover: Boolean = false)
  extends Model {

  /*
   * Remove duplicate artist
     * @return found duplicate artist if any exists
     */
  def replaceDuplicateArtist() = {
    val existingArtist = Artist.find("byName", artist.name).first.map( a => 
        //Artist name is unique
	    artist = a
    )  
  }

  /**
   * Vote for an album
   */
  def vote() = {
   nbVotes +=1
   save
  }
}

@Entity
class Artist(@Required @Column(unique = true) var name: String) extends Model {

}

object Genres {
  def values() = Array("ROCK", "POP", "BLUES", "JAZZ", "HIP-HOP", "WORLD", "OTHER")
}

//Query object for albums
object Album extends QueryOn[Album] {

  private val formatYear: SimpleDateFormat = new SimpleDateFormat("yyyy");
  def em() = JPA.em()


  /**
   * @param filter
   * @return found albums
   */
  def findAll(filter: String) = {
    var albums: List[Album] = null
    if (filter != null) {
      val likeFilter = "%".concat(filter).concat("%")
      albums = find("byNameLike", likeFilter).fetch
    }
    else albums = find("from Album").fetch(100)
    //TODO in private method for reuse
    albums.sortBy(_.nbVotes).reverse
  }

  /**
   *  first album year
   */
  def firstAlbumYear: Int = {
    def resultDate = em().createQuery("select min(a.releaseDate) from Album a").getSingleResult().asInstanceOf[Date]
    if (resultDate != null)
      return formatYear.format(resultDate).toInt
    else return 1990;
  }

  /**
   * last album year
   */
  def lastAlbumYear: Int = {
    def resultDate = em().createQuery("select max(a.releaseDate) from Album a").getSingleResult().asInstanceOf[Date]
    if (resultDate != null)
      return formatYear.format(resultDate).toInt
    else return formatYear.format(new Date()).toInt
  }

  /**
   * find albums by genre and year
   */
  def findByGenreAndYear(genre: String, year: String) = {
    var albums = find("byGenre", genre.toUpperCase).fetch
    //filter with Scala collections example
    albums = filterByYear(albums, year)
    //another scala example : sort by popularity
    albums.sortBy(_.nbVotes).reverse
  }

  /**
  * filter by year
  */
  def filterByYear (albums:List[Album], year:String) = {
	albums.filter(x => formatYear.format(x.releaseDate).equals(year))
  }

}

//Query object for artists
object Artist extends QueryOn[Artist]