#pragma once

#include "../defines.hpp"
#include "simple_tree.hpp"

#include "../geometry/rect2d.hpp"
#include "../geometry/cellid.hpp"

#include "../std/string.hpp"
#include "../std/vector.hpp"

class Reader;
class Writer;

namespace storage
{
  /// holds file name for tile and it's total size
  typedef pair<string, uint32_t> TTile;
  typedef vector<TTile> TTilesContainer;
  typedef pair<uint64_t, uint64_t> TLocalAndRemoteSize;

  /// @name Intermediate containers for data transfer
  //@{
  typedef vector<pair<uint16_t, uint32_t> > TDataFiles;
  typedef vector<pair<string, uint32_t> > TCommonFiles;
  typedef m2::CellId<9> CountryCellId;
  //@}

  bool IsTileDownloaded(TTile const & tile);

  /// Serves as a proxy between GUI and downloaded files
  class Country
  {
//    template <class TArchive> friend TArchive & operator << (TArchive & ar, storage::Country const & country);
//    template <class TArchive> friend TArchive & operator >> (TArchive & ar, storage::Country & country);

  private:
    /// Name in the coutry node tree
    string m_name;
    /// stores squares with world pieces which are part of the country
    TTilesContainer m_tiles;

  public:
    Country() {}
    Country(string const & name)
      : m_name(name) {}

    bool operator<(Country const & other) const { return Name() < other.Name(); }

    void AddTile(TTile const & tile);
    TTilesContainer const & Tiles() const { return m_tiles; }

    string Name() const { return m_name; }

    /// @return bounds for downloaded parts of the country or empty rect
    m2::RectD Bounds() const;
    TLocalAndRemoteSize Size() const;
  };

//  template <class TArchive> TArchive & operator >> (TArchive & ar, storage::Country & country)
//  {
//    ar >> country.m_name;
//    ar >> country.m_tiles;
//    return ar;
//  }

  typedef SimpleTree<Country> TCountriesContainer;

  /// @param tiles contains files and their sizes
  /// @return false if new application version should be downloaded
  bool LoadCountries(string const & countriesFile, TTilesContainer const & sortedTiles,
                     TCountriesContainer & countries);
  void SaveTiles(string const & file, int32_t level, TDataFiles const & cellFiles,
                 TCommonFiles const & commonFiles);
  bool LoadTiles(TTilesContainer & tiles, string const & tilesFile, uint32_t & dataVersion);
//  void SaveCountries(TCountriesContainer const & countries, Writer & writer);
}
