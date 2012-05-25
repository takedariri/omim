#pragma once

#include "../std/string.hpp"
#include "../std/vector.hpp"
#include "../std/utility.hpp"
#include "../std/set.hpp"
#include "../std/stdint.hpp"
#include "../std/cstdio.hpp"

namespace downloader
{

/// Single-threaded code
class ChunksDownloadStrategy
{
public:
  enum ChunkStatusT { CHUNK_FREE = 0, CHUNK_DOWNLOADING = 1, CHUNK_COMPLETE = 2, CHUNK_AUX = -1 };

private:
  struct ChunkT
  {
    /// position of chunk in file
    int64_t m_pos;
    /// @see ChunkStatusT
    int8_t m_status;

    ChunkT() : m_pos(-1), m_status(-1) {}
    ChunkT(int64_t pos, int8_t st) : m_pos(pos), m_status(st) {}
  };

  vector<ChunkT> m_chunks;

  static const int SERVER_READY = -1;
  struct ServerT
  {
    string m_url;
    int m_chunkIndex;

    ServerT(string const & url, int ind) : m_url(url), m_chunkIndex(ind) {}
  };

  vector<ServerT> m_servers;

  struct LessChunks
  {
    bool operator() (ChunkT const & r1, ChunkT const & r2) const { return r1.m_pos < r2.m_pos; }
    bool operator() (ChunkT const & r1, int64_t const & r2) const { return r1.m_pos < r2; }
    bool operator() (int64_t const & r1, ChunkT const & r2) const { return r1 < r2.m_pos; }
  };

  typedef pair<int64_t, int64_t> RangeT;
  pair<ChunkT *, int> GetChunk(RangeT const & range);

public:
  ChunksDownloadStrategy(vector<string> const & urls);

  void InitChunks(int64_t fileSize, int64_t chunkSize, ChunkStatusT status = CHUNK_FREE);
  void AddChunk(RangeT const & range, ChunkStatusT status);

  void SaveChunks(string const & fName);
  /// @return Already downloaded size.
  int64_t LoadOrInitChunks(string const & fName, int64_t fileSize, int64_t chunkSize);

  void ChunkFinished(bool success, RangeT const & range);

  enum ResultT
  {
    ENextChunk,
    ENoFreeServers,
    EDownloadFailed,
    EDownloadSucceeded
  };
  /// Should be called until returns ENextChunk
  ResultT NextChunk(string & outUrl, RangeT & range);
};

} // namespace downloader
